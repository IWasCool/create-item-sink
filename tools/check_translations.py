"""Check exact Minecraft locale coverage, translation keys, placeholders and UTF-8 resources."""
import collections
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = ROOT / "src/main/resources/assets/item_sink/lang"
MANIFEST = ROOT / "tools/localization/minecraft-1.21.1-languages.json"
PLACEHOLDER = re.compile(r"%(\d+)\$s|%%")


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate JSON key: " + key)
        result[key] = value
    return result


def read(text):
    return json.loads(text, object_pairs_hook=unique_object)


def placeholders(text):
    if "%" in PLACEHOLDER.sub("", text):
        raise ValueError("Malformed Minecraft placeholder: " + text)
    return collections.Counter(match.group(1) for match in PLACEHOLDER.finditer(text) if match.group(1))


def check():
    languages = read(MANIFEST.read_text(encoding="utf-8"))["languages"]
    files = {file.stem: file for file in LANG.glob("*.json")}
    assert set(files) == set(languages), f"Locale mismatch: missing {set(languages) - set(files)}, extra {set(files) - set(languages)}"
    reference = read(files["en_us"].read_text(encoding="utf-8"))
    signatures = {key: placeholders(value) for key, value in reference.items()}
    for locale, file in files.items():
        assert not file.read_bytes().startswith(b"\xef\xbb\xbf"), f"UTF-8 BOM: {locale}"
        data = read(file.read_text(encoding="utf-8"))
        assert data.keys() == reference.keys(), f"Key mismatch: {locale}: missing {reference.keys() - data.keys()}, extra {data.keys() - reference.keys()}"
        for key, value in data.items():
            assert isinstance(value, str) and value.strip(), f"Empty translation: {locale}:{key}"
            assert "\ufffd" not in value, f"Encoding damage: {locale}:{key}"
            assert placeholders(value) == signatures[key], f"Argument mismatch: {locale}:{key}"
            for identifier in re.findall(r"(?:minecraft|numismatics):[a-z_]+", reference[key]):
                assert identifier in value, f"Resource id changed: {locale}:{key}"
    # Every literal call site owned by this mod must resolve in all locales.
    for file in (ROOT / "src/main/java").rglob("*.java"):
        for key in re.findall(r'(?:Component\.translatable|\.translation)\("([^"\n]+)"', file.read_text(encoding="utf-8")):
            if key.endswith("."):
                continue  # dynamically constructed tier/list translation keys
            assert key in reference, f"Missing call-site key {key} in {file}"
    sound = read((LANG.parent / "sounds.json").read_text(encoding="utf-8"))
    for event in sound.values():
        if "subtitle" in event:
            assert event["subtitle"] in reference, "Untranslated sound subtitle"
    # Verify the generated artifact as well when a jar has been supplied.
    if len(sys.argv) > 1:
        with zipfile.ZipFile(sys.argv[1]) as jar:
            prefix = "assets/item_sink/lang/"
            packed = {name[len(prefix):-5] for name in jar.namelist() if name.startswith(prefix) and name.endswith(".json")}
            assert packed == set(languages), "Release jar has incomplete locale coverage"
            for locale, file in files.items():
                assert jar.read(prefix + locale + ".json") == file.read_bytes(), f"Outdated packed translation: {locale}"
    print(f"Translation checks passed: {len(files)} Minecraft locales, {len(reference)} keys per locale, "
          "matching placeholders, UTF-8, Java call sites and sound subtitles.")


if __name__ == "__main__":
    check()
