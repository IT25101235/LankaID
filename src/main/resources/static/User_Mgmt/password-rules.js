// These rules match User_Mgmt/Service/PasswordRules.java.
function passwordError(value) {
  if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\s])\S{8,72}$/.test(value)) {
    return 'Use 8 to 72 characters with an uppercase letter, a lowercase letter, a number and a symbol, without spaces.';
  }
  if (new TextEncoder().encode(value).length > 72) {
    return 'Password is too long. Use fewer non-English characters (maximum 72 UTF-8 bytes).';
  }
  return '';
}
