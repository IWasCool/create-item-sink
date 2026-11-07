# Localization sources

`minecraft-1.21.1-languages.json` records the 137 language options from Minecraft's own `pack.mcmeta` in asset index 17. Its asset hash is recorded for provenance. The index is identified by the local Minecraft 1.21.1 version manifest.

`terms.tsv` contains authored translation terms for the mod. `variants.json` explicitly identifies regional variants that share vocabulary, plus transformations for Belarusian Latin, Serbian Cyrillic, older Indonesian spelling, Swiss German spelling, and upside-down English. Pirate Speak, Anglish, Shakespearean English, LOLCAT, and other constructed language options have their own entries.

These are initial AI-authored translations, with no claim of native-speaker verification. Dialect, minority-language, and constructed-language entries especially need native review. Ngiiwa (`pls`) and Tsotsil (`tzo_mx`) use concise symbolic labels and Minecraft's own translated settings, inventory, completion, and yes/no vocabulary for specialist concepts rather than inventing technical words. These two locales need fuller native translations.

Regenerate with `py tools/generate_translations.py`. Individual JSON files remain normal Minecraft language resources and can be edited directly; update the term source too when those edits must survive regeneration. Validate with `py tools/check_translations.py` and build with `./gradlew.bat build`.

`Create`, `CreateCore`, currency mod names, config filenames, FE, and standardized unit notation remain identifiers. The language system handles right-to-left text using Minecraft's existing locale metadata; the mod does not reverse Arabic, Persian, Hebrew, Yiddish, or Jawi text manually.

`jei-overview.tsv` contains the five JEI overview strings for all 117 base locales. The 20 regional and spelling/script variants are derived with the same rules as the main glossary. Run `py tools/generate_translations.py --overview-only` to update just these keys and preserve every established translation. All 137 files must contain all 87 keys; `check_translations.py` no longer permits missing overview keys or relies on English fallback.

The overview text is also an initial AI-authored draft, not native-speaker verified. Ngiiwa and Tsotsil retain symbolic technical text using the existing native vocabulary; Quenya and Volapük also use concise value/conversion diagrams. Full prose and idiom review are still needed for these locales. Regeneration preserves independently maintained Ponder captions and locale translations.
