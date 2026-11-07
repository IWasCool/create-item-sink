"""Inventory every CreateCore jar, including bundled jars, and prepare an isolated registry scan.

Run with Python 3.11+. No jars in the source project are changed.
"""
import argparse
import io
import json
import re
import shutil
import tomllib
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def required_dependency(dependency):
    return dependency.get("type", "required" if dependency.get("mandatory", dependency.get("required", True))
                          else "optional") == "required" and dependency.get("side", "BOTH") != "CLIENT"


def inspect_archive(data, source, records):
    with zipfile.ZipFile(data) as archive:
        names = set(archive.namelist())
        metadata = {}
        for name in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml"):
            if name in names:
                metadata = tomllib.loads(archive.read(name).decode("utf-8-sig"))
                break
        models = sorted(name for name in names if re.fullmatch(r"assets/[^/]+/models/item/.+\.json", name))
        records.append({
            "source": source,
            "mods": [{"id": mod["modId"], "version": mod.get("version", "")}
                     for mod in metadata.get("mods", [])],
            "dependencies": sorted({dep["modId"] for deps in metadata.get("dependencies", {}).values()
                                    for dep in deps if required_dependency(dep)}),
            "model_count": len(models),
            "model_namespaces": sorted({name.split("/")[1] for name in models}),
        })
        if "META-INF/jarjar/metadata.json" in names:
            for child in json.loads(archive.read("META-INF/jarjar/metadata.json")).get("jars", []):
                nested = child["path"]
                inspect_archive(io.BytesIO(archive.read(nested)), source + "!" + nested, records)


def prepare(core, legacy_config=None):
    records = []
    jars = sorted((core / "libs").glob("*.jar"))
    for jar in jars:
        inspect_archive(jar, jar.name, records)
    # Load server mods even when they supply no models: items can use custom
    # renderers or another mod's assets. Client utilities are still inventoried.
    client_utilities = {
        "ambientsounds", "appleskin", "bcc", "chloride", "cloth_config", "collapsible_groups",
        "colorwheel", "configured", "controlling", "defaultoptions", "drippyloadingscreen",
        "dynamic_fps", "emotecraft", "enchdesc", "entity_model_features", "entity_texture_features",
        "entityculling", "euphoria_patcher", "fancymenu", "fancytoasts", "fusion", "immediatelyfast",
        "iris", "item_icon_exporter", "jade", "jadeaddons", "jei", "jeresources", "konkrete",
        "lambdynlights", "melody", "moreoverlays", "mousetweaks", "nerb", "notenoughanimations",
        "ok_zoomer", "particular", "reforgedplaymod", "replaymod", "sablereplay", "searchables",
        "simplerpc", "skinlayers3d", "sodium", "sound_physics_remastered", "weaponmaster_ydm",
        "xaerominimap", "xaeroworldmap",
    }
    selected = {row["source"] for row in records if "!" not in row["source"]
                and not any(mod["id"].lower() in client_utilities for mod in row["mods"])}
    # Include any content provider irrespective of client utility classification.
    selected.update(row["source"].split("!")[0] for row in records if row["model_count"])
    # Language providers use service metadata instead of a regular @Mod entry.
    selected.update(jar.name for jar in jars if jar.name.startswith("kotlinforforge-"))
    excluded = {"create_item_sink", "item_sink", "item_icon_exporter"}
    providers = {}
    for row in records:
        for mod in row["mods"]:
            version = tuple(int(n) for n in re.findall(r"\d+", mod["version"]))
            old = providers.get(mod["id"])
            if old is None or version > old[0]:
                providers[mod["id"]] = (version, row["source"].split("!")[0])
    selected = {name for name in selected if not any(mod["id"] in excluded
                for row in records if row["source"] == name for mod in row["mods"])}
    # Choose the newest jar when Core's libs contains duplicate mod versions.
    selected = {name for name in selected if all(providers[mod["id"]][1] == name
                for row in records if row["source"] == name for mod in row["mods"])}
    while True:
        before = set(selected)
        for row in records:
            if row["source"].split("!")[0] in selected:
                for dependency in row["dependencies"]:
                    if dependency in providers and dependency not in excluded:
                        selected.add(providers[dependency][1])
        if before == selected:
            break
    output = ROOT / "build/value-scan-mods"
    output.mkdir(parents=True, exist_ok=True)
    player_jars = [jar for jar in sorted((core.parent / "playerblocks-1.21.1/build/libs").glob("*.jar"))
                   if not jar.name.endswith("-sources.jar")]
    expected = selected | {jar.name for jar in player_jars}
    # Only remove stale scan copies in this specific generated directory.
    for stale in output.glob("*.jar"):
        if stale.name not in expected and stale.resolve().parent == output.resolve():
            stale.unlink()
    for jar in jars:
        if jar.name in selected:
            shutil.copy2(jar, output / jar.name)
    # Core's standalone Player Blocks dependency is supplied outside libs.
    for jar in player_jars:
        shutil.copy2(jar, output / jar.name)
        inspect_archive(jar, "../playerblocks-1.21.1/build/libs/" + jar.name, records)
    inventory = {"core_project": core.name, "top_level_jars": len(jars),
                 "scan_jars": sorted(selected), "archives": records}
    target = ROOT / "docs/value-mod-inventory.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(inventory, indent=2) + "\n", encoding="utf-8")
    # Several Create addons have an upstream parallel-registration race.
    config = ROOT / "build/value-scan-run/config"
    config.mkdir(parents=True, exist_ok=True)
    (config / "fml.toml").write_text("maxThreads = 1\n", encoding="utf-8")
    if legacy_config is not None:
        shutil.copy2(legacy_config, config / "item_sink-common.toml")
    print(f"Inventoried {len(jars)} jars and {len(records) - len(jars)} bundled/dependency archives; "
          f"prepared {len(selected) + len(player_jars)} content/dependency jars.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--core", type=Path, default=ROOT.parent / "createnewworldcore-1.21.1")
    parser.add_argument("--legacy-config", type=Path, help="Seed the isolated run with an existing Item Sink config")
    args = parser.parse_args()
    prepare(args.core.resolve(), args.legacy_config)
