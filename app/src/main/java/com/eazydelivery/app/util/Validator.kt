package com.eazydelivery.app.util

import android.content.Context
import android.util.Patterns
import com.eazydelivery.app.R
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for validating user inputs
 */
@Singleton
class Validator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Validates an email address
     */
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult(false, context.getString(R.string.email_empty))
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> ValidationResult(false, context.getString(R.string.email_invalid))
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates a password
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult(false, context.getString(R.string.password_empty))
            password.length < 6 -> ValidationResult(false, context.getString(R.string.password_too_short))
            !password.any { it.isDigit() } -> ValidationResult(false, context.getString(R.string.password_no_digit))
            !password.any { it.isLetter() } -> ValidationResult(false, context.getString(R.string.password_no_letter))
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates a phone number (primarily for Indian numbers, but with basic support for other formats)
     *
     * @param phoneNumber The phone number to validate (without country code)
     * @param regionCode The ISO 3166-1 two-letter region code (default: "IN" for India)
     * @return ValidationResult indicating if the phone number is valid
     */
    fun validatePhoneNumber(phoneNumber: String, regionCode: String = "IN"): ValidationResult {
        if (phoneNumber.isBlank()) {
            return ValidationResult(false, context.getString(R.string.phone_number_empty))
        }

        // Remove any whitespace or formatting characters
        val cleanedNumber = phoneNumber.replace("\\s".toRegex(), "")
                                      .replace("-", "")
                                      .replace("(", "")
                                      .replace(")", "")

        // For Indian numbers, perform additional validation
        if (regionCode == "IN") {
            // Check if the phone number is exactly 10 digits for Indian numbers
            if (cleanedNumber.length != Constants.PHONE_NUMBER_LENGTH) {
                return ValidationResult(false, context.getString(R.string.phone_number_length_invalid))
            }

            if (!cleanedNumber.all { it.isDigit() }) {
                return ValidationResult(false, context.getString(R.string.phone_number_digits_only))
            }

            // Check if the phone number starts with a valid Indian mobile prefix (6, 7, 8, 9)
            val firstDigit = cleanedNumber.first().toString().toInt()
            if (firstDigit < Constants.MIN_VALID_INDIAN_PREFIX) {
                return ValidationResult(false, context.getString(R.string.phone_number_invalid_prefix))
            }
        }

        // Use libphonenumber for validation
        try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val phoneNumberProto = phoneUtil.parse(cleanedNumber, regionCode)
            if (!phoneUtil.isValidNumber(phoneNumberProto)) {
                return ValidationResult(false, context.getString(R.string.phone_number_invalid))
            }
        } catch (e: NumberParseException) {
            Timber.e(e, "Error validating phone number")
            return ValidationResult(false, context.getString(R.string.phone_number_invalid))
        }

        // If all checks pass, the phone number is valid
        return ValidationResult(true)
    }

    /**
     * Validates a phone number with country code prefix
     *
     * @param fullPhoneNumber The full phone number including country code prefix (e.g., "+919876543210")
     * @param defaultRegion The default region to assume if no international prefix is present
     * @return ValidationResult indicating if the phone number is valid
     */
    fun validateFullPhoneNumber(fullPhoneNumber: String, defaultRegion: String = "IN"): ValidationResult {
        if (fullPhoneNumber.isBlank()) {
            return ValidationResult(false, context.getString(R.string.phone_number_empty))
        }

        // Remove any whitespace or formatting characters
        val cleanedNumber = fullPhoneNumber.replace("\\s".toRegex(), "")
                                         .replace("-", "")
                                         .replace("(", "")
                                         .replace(")", "")

        // Use libphonenumber for full validation with country code
        try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val phoneNumberProto = phoneUtil.parse(cleanedNumber, defaultRegion)

            if (!phoneUtil.isValidNumber(phoneNumberProto)) {
                return ValidationResult(false, context.getString(R.string.phone_number_invalid))
            }

            // Get the region code for this number
            val regionCode = phoneUtil.getRegionCodeForNumber(phoneNumberProto)

            // For Indian numbers, perform additional validation
            if (regionCode == "IN") {
                // Check if the phone number has the correct country code
                val countryCode = phoneUtil.getCountryCodeForRegion("IN")
                if (phoneNumberProto.countryCode != countryCode) {
                    return ValidationResult(false, context.getString(R.string.phone_number_invalid_country_code))
                }

                // Get the national number (without country code)
                val nationalNumber = phoneNumberProto.nationalNumber.toString()

                // Validate the national number using our specific rules for India
                if (nationalNumber.length != Constants.PHONE_NUMBER_LENGTH) {
                    return ValidationResult(false, context.getString(R.string.phone_number_length_invalid))
                }

                // Check if the phone number starts with a valid Indian mobile prefix (6, 7, 8, 9)
                val firstDigit = nationalNumber.first().toString().toInt()
                if (firstDigit < Constants.MIN_VALID_INDIAN_PREFIX) {
                    return ValidationResult(false, context.getString(R.string.phone_number_invalid_prefix))
                }
            }

            // If we've made it this far, the number is valid
            return ValidationResult(true)
        } catch (e: NumberParseException) {
            Timber.e(e, "Error validating full phone number")
            return ValidationResult(false, context.getString(R.string.phone_number_invalid))
        }
    }

    /**
     * Formats a phone number with proper spacing and country code
     *
     * @param phoneNumber The phone number to format (without country code)
     * @param regionCode The region code to use (default: "IN" for India)
     * @param formatType The format type to use (default: INTERNATIONAL)
     * @return The formatted phone number or null if invalid
     */
    fun formatPhoneNumber(
        phoneNumber: String,
        regionCode: String = "IN",
        formatType: PhoneNumberUtil.PhoneNumberFormat = PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
    ): String? {
        // Clean the phone number
        val cleanedNumber = phoneNumber.replace("\\s".toRegex(), "")
                                     .replace("-", "")
                                     .replace("(", "")
                                     .replace(")", "")

        // Validate the phone number first
        val validationResult = validatePhoneNumber(cleanedNumber, regionCode)
        if (!validationResult.isValid) {
            return null
        }

        try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val numberProto = phoneUtil.parse(cleanedNumber, regionCode)

            // Format according to the requested format
            return phoneUtil.format(numberProto, formatType)
        } catch (e: NumberParseException) {
            Timber.e(e, "Error formatting phone number")

            // Fallback to simple formatting for Indian numbers
            if (regionCode == "IN" && cleanedNumber.length == Constants.PHONE_NUMBER_LENGTH) {
                val countryCode = Constants.INDIA_COUNTRY_CODE
                return "$countryCode ${cleanedNumber.substring(0, 5)} ${cleanedNumber.substring(5)}"
            }

            return null
        }
    }

    /**
     * Formats a full phone number (with country code) with proper spacing
     *
     * @param fullPhoneNumber The full phone number including country code
     * @param defaultRegion The default region to assume if no international prefix is present
     * @param formatType The format type to use (default: INTERNATIONAL)
     * @return The formatted phone number or null if invalid
     */
    fun formatFullPhoneNumber(
        fullPhoneNumber: String,
        defaultRegion: String = "IN",
        formatType: PhoneNumberUtil.PhoneNumberFormat = PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
    ): String? {
        // Clean the phone number
        val cleanedNumber = fullPhoneNumber.replace("\\s".toRegex(), "")
                                         .replace("-", "")
                                         .replace("(", "")
                                         .replace(")", "")

        try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val numberProto = phoneUtil.parse(cleanedNumber, defaultRegion)

            if (!phoneUtil.isValidNumber(numberProto)) {
                return null
            }

            // Format according to the requested format
            return phoneUtil.format(numberProto, formatType)
        } catch (e: NumberParseException) {
            Timber.e(e, "Error formatting full phone number")
            return null
        }
    }

    /**
     * Compares two phone numbers to check if they are the same
     * This is useful for confirming phone number changes
     *
     * @param phoneNumber1 The first phone number
     * @param phoneNumber2 The second phone number
     * @param defaultRegion The default region to assume if no international prefix is present
     * @return true if the phone numbers are the same, false otherwise
     */
    fun arePhoneNumbersEqual(phoneNumber1: String, phoneNumber2: String, defaultRegion: String = "IN"): Boolean {
        try {
            val phoneUtil = PhoneNumberUtil.getInstance()

            // Parse both phone numbers
            val proto1 = phoneUtil.parse(phoneNumber1, defaultRegion)
            val proto2 = phoneUtil.parse(phoneNumber2, defaultRegion)

            // Check if both numbers are valid
            if (!phoneUtil.isValidNumber(proto1) || !phoneUtil.isValidNumber(proto2)) {
                return false
            }

            // Compare the phone numbers
            return phoneUtil.isNumberMatch(proto1, proto2) == PhoneNumberUtil.MatchType.EXACT_MATCH
        } catch (e: NumberParseException) {
            Timber.e(e, "Error comparing phone numbers")
            return false
        }
    }

    /**
     * Validates a name
     */
    fun validateName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, context.getString(R.string.name_empty))
            name.length < 2 -> ValidationResult(false, context.getString(R.string.name_too_short))
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates an amount
     */
    fun validateAmount(amount: String): ValidationResult {
        return try {
            val amountValue = amount.toDouble()
            when {
                amountValue <= 0 -> ValidationResult(false, context.getString(R.string.amount_negative))
                else -> ValidationResult(true)
            }
        } catch (e: NumberFormatException) {
            ValidationResult(false, context.getString(R.string.amount_invalid))
        }
    }
}

/**
 * Result of a validation operation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
