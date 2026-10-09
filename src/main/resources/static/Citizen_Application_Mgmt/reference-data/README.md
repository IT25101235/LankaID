# Birth-location reference data

The citizen application forms load these files locally. No external API is used at runtime.

- `sri-lanka-locations.json` contains each Sri Lankan district with its divisional secretariats and cities/towns.
- `countries.json` contains the selectable country list. A foreign country uses free-text region and city fields because a complete worldwide city hierarchy would be difficult to keep accurate offline.

To update the data, keep the existing JSON shape and names, update `dataVersion`, and run the form-validation and citizen workflow tests. The server validates submitted values against the same files, so browser and backend rules remain consistent.

Source attribution is stored in each JSON file. The Sri Lankan hierarchy uses Department of Census and Statistics/Open Admin Data material, with town names from SriLankaCitiesDatabase. The country list is based on ISO 3166 data packaged by `world_countries`.
