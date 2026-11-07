"""Generate every Minecraft 1.21.1 locale from the maintained translation glossary."""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / "tools/localization"
OUTPUT = ROOT / "src/main/resources/assets/item_sink/lang"
FIELDS = ("sink", "conversion", "settings", "capacity", "max_input", "require_energy",
          "values", "output_item", "output_count", "processing_time", "energy_per_tick",
          "include_values", "excluded_items", "progress", "complete", "motor_sound",
          "no_outputs", "tier", "tier_items", "ticks")
TIERS = (1, 2, 3, 4, 5, 6, 7, 8, 16, 32, 48, 64)
PREFIX = "item_sink.configuration."
OVERVIEW_KEYS = ("jei.item_sink.any_item", "jei.item_sink.currency", "jei.item_sink.overview.accepted",
                 "jei.item_sink.overview.payout", "jei.item_sink.overview.excluded")


def overview_glossary():
    result = {}
    for number, line in enumerate((SOURCES / "jei-overview.tsv").read_text(encoding="utf-8").splitlines(), 1):
        if not line or line.startswith("#"):
            continue
        locale, *terms = line.split("|")
        if len(terms) != len(OVERVIEW_KEYS) or locale in result or not all(term.strip() for term in terms):
            raise ValueError(f"Invalid or duplicate JEI glossary row {number}: {locale}")
        result[locale] = dict(zip(OVERVIEW_KEYS, terms))
    if result.keys() != glossary().keys():
        raise ValueError("JEI overview glossary must cover every base locale exactly once")
    return result


def glossary():
    result = {}
    for number, line in enumerate((SOURCES / "terms.tsv").read_text(encoding="utf-8").splitlines(), 1):
        if not line or line.startswith("#"):
            continue
        locale, *terms = line.split("|")
        if len(terms) != len(FIELDS) or locale in result:
            raise ValueError(f"Invalid or duplicate glossary row {number}: {locale}")
        result[locale] = dict(zip(FIELDS, terms))
    return result


def make_catalogue(terms):
    brand = "Create: " + terms["sink"]
    title = terms["sink"] + " — " + terms["settings"]
    result = {
        "itemGroup.item_sink": brand,
        "block.item_sink.item_sink": terms["sink"],
        "jei.item_sink.item_sink_conversion": terms["conversion"],
        "jei.item_sink.details": "%1$ss  %2$s FE/t",
        "gui.item_sink.energy": "%1$s / %2$s FE",
        "gui.item_sink.progress": terms["progress"] + ": %1$s / %2$s " + terms["ticks"],
        "gui.item_sink.complete": "%1$s%% " + terms["complete"],
        "message.item_sink.no_valid_outputs": terms["no_outputs"] + ". %1$s / %2$s (%3$s).",
        "subtitles.item_sink.electric_motor_buzz": terms["motor_sound"],
        PREFIX + "title": title,
        PREFIX + "section.item_sink.common.toml": title,
        PREFIX + "section.item_sink.common.toml.title": title,
        PREFIX + "item_sink": terms["sink"],
        PREFIX + "item_sink.values": terms["values"],
    }
    properties = {
        "energyCapacity": ("capacity", "FE"),
        "maxEnergyInput": ("max_input", "FE/t"),
        "requireEnergy": ("require_energy", "FE"),
        "values.includeCreateCoreValues": ("include_values", "CreateCore"),
        "values.excludedItems": ("excluded_items", "minecraft:rotten_flesh"),
        "values.outputItem": ("output_item", "numismatics:spur / minecraft:emerald"),
        "values.outputCount": ("output_count", "1…64"),
        "values.ticks": ("processing_time", "20 " + terms["ticks"] + " = 1 s"),
        "values.energyPerTick": ("energy_per_tick", "FE/t"),
    }
    for path, (field, example) in properties.items():
        key = PREFIX + "item_sink." + path
        result[key] = terms[field]
        result[key + ".tooltip"] = terms[field] + "\n" + example
    for tier in TIERS:
        key = PREFIX + "item_sink.values.value" + str(tier)
        result[key] = terms["tier"].replace("%s", str(tier))
        result[key + "Items"] = terms["tier_items"].replace("%s", str(tier))
        result[key + "Items.tooltip"] = result[key + "Items"] + "\nminecraft:rotten_flesh"
    return result


def transliterate(text, lower, upper):
    mapping = dict(lower)
    mapping.update(upper)
    # Long digraphs must be replaced before single letters.
    pattern = "|".join(re.escape(key) for key in sorted(mapping, key=len, reverse=True))
    return re.sub(pattern, lambda match: mapping[match.group()], text)


def variant(text, transform):
    if transform == "swiss_german":
        return text.replace("ß", "ss").replace("ẞ", "SS")
    if transform == "old_indonesian":
        return text.replace("j", "dj").replace("J", "Dj").replace("y", "j").replace("Y", "J").replace("u", "oe").replace("U", "Oe").replace("c", "tj").replace("C", "Tj")
    if transform == "belarusian_latin":
        lower = dict(zip("абвгдеёжзійклмнопрстуўфхцчшыьэюя", (
            "a", "b", "v", "h", "d", "je", "jo", "ž", "z", "i", "j", "k", "l", "m", "n", "o",
            "p", "r", "s", "t", "u", "ŭ", "f", "ch", "c", "č", "š", "y", "", "e", "ju", "ja")))
        # Soft consonants use the standard Belarusian Latin diacritics.
        lower.update({"нь": "ń", "ль": "l", "ць": "ć", "сь": "ś", "зь": "ź"})
        upper = {source.capitalize(): target.capitalize() for source, target in lower.items()}
        return transliterate(text, lower, upper)
    if transform == "serbian_cyrillic":
        lower = dict(zip("abvgdđezžijk lmnoprstćufhcčš".replace(" ", ""), "абвгдђезжијклмнопрстћуфхцчш"))
        lower.update({"lj": "љ", "nj": "њ", "dž": "џ"})
        upper = {source.capitalize(): target.upper() for source, target in lower.items()}
        return transliterate(text, lower, upper)
    if transform == "upside_down":
        mapping = str.maketrans("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,!?()[]",
                               "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz∀𐐒ƆᗡƎℲ⅁HIſʞ˥WNOԀΌᴚS⊥∩ΛMX⅄Z0ƖᄅƐㄣϛ9ㄥ86˙'¡¿)(][")
        # Treat placeholders and escaped percentages as atomic tokens when turning
        # text around. Minecraft must still be able to substitute its arguments.
        tokens = re.findall(r"%\d+\$s|%%|minecraft:[a-z_]+|numismatics:[a-z_]+|CreateCore|Create|FE/t|FE|.", text, re.S)
        return "".join(token if len(token) > 1 else token.translate(mapping) for token in reversed(tokens))
    raise ValueError("Unknown locale transformation: " + transform)


def generate(overview_only=False):
    languages = json.loads((SOURCES / "minecraft-1.21.1-languages.json").read_text(encoding="utf-8"))["languages"]
    terms = glossary()
    variants = json.loads((SOURCES / "variants.json").read_text(encoding="utf-8"))
    if set(languages) != set(terms) | set(variants) or set(terms) & set(variants):
        raise ValueError("Glossary and variant locales do not match Minecraft's language manifest")
    overview = overview_glossary()
    catalogues = {locale: {**make_catalogue(row), **overview[locale]} for locale, row in terms.items()}
    # Preserve the established English names and full troubleshooting instruction.
    for key in (PREFIX + "title", PREFIX + "section.item_sink.common.toml", PREFIX + "section.item_sink.common.toml.title"):
        catalogues["en_us"][key] = "Item Sink Configs"
    catalogues["en_us"]["message.item_sink.no_valid_outputs"] = (
        "Item Sink has no valid conversion outputs. Install %1$s or edit %2$s in %3$s.")
    for locale, settings in variants.items():
        catalogue = dict(catalogues[settings["base"]])
        if "transform" in settings:
            for key, value in catalogue.items():
                # Proper names, resource ids, FE, numeric placeholders and unit
                # notation stay intact under spelling/script transformations.
                if key in {"gui.item_sink.energy", "jei.item_sink.details"}:
                    continue
                transform = settings["transform"]
                if transform == "upside_down":
                    catalogue[key] = variant(value, transform)
                else:
                    parts = re.split(r"(CreateCore|Create|FE|minecraft:[a-z_]+|numismatics:[a-z_]+|%\d+\$s)", value)
                    catalogue[key] = "".join(part if index % 2 else variant(part, transform)
                                             for index, part in enumerate(parts))
        catalogues[locale] = catalogue
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for locale, catalogue in sorted(catalogues.items()):
        if overview_only:
            catalogue = {key: catalogue[key] for key in OVERVIEW_KEYS}
        # Keep independently maintained Ponder captions and translations.
        existing = OUTPUT / (locale + ".json")
        if existing.exists():
            catalogue = {**json.loads(existing.read_text(encoding="utf-8")), **catalogue}
        (OUTPUT / (locale + ".json")).write_text(json.dumps(catalogue, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(catalogues)} locale files; existing Ponder captions and optional translations preserved.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--overview-only", action="store_true", help="Update only the five JEI strings, preserving every established translation")
    generate(overview_only=parser.parse_args().overview_only)
