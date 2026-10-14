// Shared by the NIC and passport forms, including renewals and corrections.
// Runs once when the form script is loaded.
(() => {
  const form = document.querySelector('form[data-application-form]');
  if (!form) return;

  // Adds live letter-only validation to each name field.
  ['familyName', 'givenName', 'surname'].forEach(name => {
    const input = form.elements.namedItem(name);
    if (!input) return;
    // Updates the browser validation message while the citizen types.
    input.addEventListener('input', () => {
      const valid = !input.value || /^\p{L}[\p{L}\p{M} ]*$/u.test(input.value);
      input.setCustomValidity(valid ? '' : 'Enter letters and spaces only; remove numbers and symbols.');
    });
  });

  // Adds five-digit validation to both postal-code fields.
  ['permanentPostalCode', 'postalPostalCode'].forEach(name => {
    const input = form.elements.namedItem(name);
    if (!input) return;
    // Updates the postal-code validation message while the citizen types.
    input.addEventListener('input', () => {
      input.setCustomValidity(!input.value || /^[0-9]{5}$/.test(input.value)
        ? '' : 'Enter exactly 5 digits for the postal code.');
    });
  });

  // Shows or hides an optional section and updates its input requirements.
  function setSection(id, active) {
    const section = document.getElementById(id);
    if (!section) return;
    section.classList.toggle('hidden', !active);
    // Applies the enabled and required state to every input in the section.
    section.querySelectorAll('input').forEach(input => {
      input.disabled = !active;
      input.required = active;
    });
  }

  // Refreshes optional sections from the current checkbox choices.
  function updateSections() {
    const samePostal = document.getElementById('samePostal');
    const dualCitizen = document.getElementById('dualCitizen');
    setSection('postalAddress', samePostal ? !samePostal.checked : false);
    setSection('dualDetails', dualCitizen ? dualCitizen.checked : false);
  }
  document.getElementById('samePostal')?.addEventListener('change', updateSections);
  document.getElementById('dualCitizen')?.addEventListener('change', updateSections);
  updateSections();

  // Load project-local reference data and switch between strict Sri Lankan dropdowns
  // and validated free-text fields for foreign births.
  const country = document.getElementById('countryOfBirth');
  const sriDistrictField = document.getElementById('sriBirthDistrictField');
  const sriDivisionField = document.getElementById('sriBirthDivisionField');
  const sriCityField = document.getElementById('sriBirthCityField');
  const foreignRegionField = document.getElementById('foreignBirthRegionField');
  const foreignCityField = document.getElementById('foreignBirthCityField');
  const sriDistrict = document.getElementById('sriBirthDistrict');
  const sriDivision = document.getElementById('sriBirthDivision');
  const sriCity = document.getElementById('sriBirthCity');
  const foreignRegion = document.getElementById('foreignBirthRegion');
  const foreignCity = document.getElementById('foreignBirthCity');

  const replaceOptions = (select, prompt, values, selected) => {
    select.replaceChildren(new Option(prompt, ''));
    // Sort locally so future JSON edits cannot make any dropdown difficult to scan.
    [...values].sort((left, right) => left.localeCompare(right))
      .forEach(value => select.add(new Option(value, value, false, value === selected)));
  };

  const loadBirthLocations = async () => {
    if (!country || !sriDistrict || !sriDivision || !sriCity
      || !foreignRegion || !foreignCity) {
      throw new Error('Location fields could not be initialized. Refresh this page.');
    }
    const [countryResponse, locationResponse] = await Promise.all([
      fetch('/Citizen_Application_Mgmt/reference-data/countries.json'),
      fetch('/Citizen_Application_Mgmt/reference-data/sri-lanka-locations.json')
    ]);
    if (!countryResponse.ok || !locationResponse.ok) {
      throw new Error('Location reference data is unavailable.');
    }

    const countryData = await countryResponse.json();
    const locationData = await locationResponse.json();
    const districts = locationData.districts;
    const initialCountry = country.dataset.initial || 'Sri Lanka';
    const initialDistrict = sriDistrict.dataset.initial || '';
    const initialDivision = sriDivision.dataset.initial || '';
    const initialCity = sriCity.dataset.initial || '';

    replaceOptions(country, 'Select country', countryData.countries.map(item => item.name), initialCountry);
    country.value = countryData.countries.some(item => item.name === initialCountry)
      ? initialCountry : 'Sri Lanka';

    const populateDistricts = selected => {
      replaceOptions(sriDistrict, 'Select district', districts.map(item => item.name), selected);
    };

    const populateDistrictChildren = (districtName, selectedDivision = '', selectedCity = '') => {
      const district = districts.find(item => item.name === districtName);
      replaceOptions(sriDivision, 'Select division', district ? district.divisions : [], selectedDivision);
      replaceOptions(sriCity, 'Select city or town', district ? district.cities : [], selectedCity);
    };

    const setBirthMode = (clearChangedValues = false) => {
      const sriLankan = country.value === 'Sri Lanka';
      [sriDistrictField, sriDivisionField, sriCityField].forEach(field => field.classList.toggle('hidden', !sriLankan));
      [foreignRegionField, foreignCityField].forEach(field => field.classList.toggle('hidden', sriLankan));
      [sriDistrict, sriDivision, sriCity].forEach(field => {
        field.disabled = !sriLankan;
        field.required = sriLankan;
      });
      [foreignRegion, foreignCity].forEach(field => {
        field.disabled = sriLankan;
        field.required = !sriLankan;
      });

      if (clearChangedValues) {
        if (sriLankan) {
          foreignRegion.value = '';
          foreignCity.value = '';
          populateDistricts('');
          populateDistrictChildren('');
        } else {
          sriDistrict.value = '';
          populateDistrictChildren('');
          foreignRegion.value = '';
          foreignCity.value = '';
        }
      }
    };

    populateDistricts(initialDistrict);
    populateDistrictChildren(initialDistrict, initialDivision, initialCity);
    setBirthMode(false);

    country.addEventListener('change', () => setBirthMode(true));
    sriDistrict.addEventListener('change', () => populateDistrictChildren(sriDistrict.value));
  };

  loadBirthLocations().catch(error => {
    if (country) {
      country.setCustomValidity(error.message);
      country.replaceChildren(new Option('Location data unavailable — refresh page', ''));
    }
  });

  const today = new Date();
  // Formats a JavaScript date for an HTML date input.
  const localDate = date => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  // Prevents form date fields from accepting future dates.
  form.querySelectorAll('input[type="date"]').forEach(input => { input.max = localDate(today); });
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  const dateOfBirth = form.elements.namedItem('dateOfBirth');
  if (dateOfBirth) dateOfBirth.max = localDate(yesterday);

  if (form.action.includes('/citizen/applications/nic/')) {
    const minimumNicAge = new Date(today);
    minimumNicAge.setFullYear(today.getFullYear() - 15);
    if (dateOfBirth) dateOfBirth.max = localDate(minimumNicAge);
  }
})();
