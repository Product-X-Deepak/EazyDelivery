package com.eazydelivery.app.util

import android.content.Context
import com.eazydelivery.app.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ValidatorTest {

    @Mock
    private lateinit var context: Context

    private lateinit var validator: Validator

    @Before
    fun setup() {
        // Mock string resources
        `when`(context.getString(R.string.phone_number_empty)).thenReturn("Phone number cannot be empty")
        `when`(context.getString(R.string.phone_number_invalid)).thenReturn("Please enter a valid phone number")
        `when`(context.getString(R.string.phone_number_length_invalid)).thenReturn("Phone number must be exactly 10 digits")
        `when`(context.getString(R.string.phone_number_digits_only)).thenReturn("Phone number must contain only digits")
        `when`(context.getString(R.string.phone_number_invalid_prefix)).thenReturn("Phone number must start with 6, 7, 8, or 9")
        `when`(context.getString(R.string.phone_number_invalid_country_code)).thenReturn("Phone number must start with +91 (India)")

        validator = Validator(context)
    }

    @Test
    fun `validatePhoneNumber should return valid for valid Indian phone numbers`() {
        // Valid Indian phone numbers
        assert(validator.validatePhoneNumber("9876543210").isValid)
        assert(validator.validatePhoneNumber("8765432109").isValid)
        assert(validator.validatePhoneNumber("7654321098").isValid)
        assert(validator.validatePhoneNumber("6543210987").isValid)
    }

    @Test
    fun `validatePhoneNumber should return invalid for invalid Indian phone numbers`() {
        // Invalid Indian phone numbers
        assert(!validator.validatePhoneNumber("987654321").isValid) // Too short
        assert(!validator.validatePhoneNumber("98765432101").isValid) // Too long
        assert(!validator.validatePhoneNumber("5876543210").isValid) // Invalid prefix (starts with 5)
        assert(!validator.validatePhoneNumber("4876543210").isValid) // Invalid prefix (starts with 4)
        assert(!validator.validatePhoneNumber("3876543210").isValid) // Invalid prefix (starts with 3)
        assert(!validator.validatePhoneNumber("2876543210").isValid) // Invalid prefix (starts with 2)
        assert(!validator.validatePhoneNumber("1876543210").isValid) // Invalid prefix (starts with 1)
        assert(!validator.validatePhoneNumber("0876543210").isValid) // Invalid prefix (starts with 0)
        assert(!validator.validatePhoneNumber("abcdefghij").isValid) // Non-numeric
        assert(!validator.validatePhoneNumber("98765-4321").isValid) // Contains non-digits
    }

    @Test
    fun `validateFullPhoneNumber should return valid for valid Indian phone numbers with country code`() {
        // Valid Indian phone numbers with country code
        assert(validator.validateFullPhoneNumber("+919876543210").isValid)
        assert(validator.validateFullPhoneNumber("+918765432109").isValid)
        assert(validator.validateFullPhoneNumber("+917654321098").isValid)
        assert(validator.validateFullPhoneNumber("+916543210987").isValid)
    }

    @Test
    fun `validateFullPhoneNumber should return invalid for invalid phone numbers with country code`() {
        // Invalid phone numbers with country code
        assert(!validator.validateFullPhoneNumber("+91987654321").isValid) // Too short
        assert(!validator.validateFullPhoneNumber("+9198765432101").isValid) // Too long
        assert(!validator.validateFullPhoneNumber("+915876543210").isValid) // Invalid prefix
        assert(!validator.validateFullPhoneNumber("+81987654321").isValid) // Wrong country code
        assert(!validator.validateFullPhoneNumber("+1987654321").isValid) // Wrong country code
        assert(!validator.validateFullPhoneNumber("+").isValid) // Just a plus
        assert(!validator.validateFullPhoneNumber("9876543210").isValid) // No country code
        assert(!validator.validateFullPhoneNumber("+91abcdefghij").isValid) // Non-numeric
    }

    @Test
    fun `formatPhoneNumber should correctly format valid Indian phone numbers`() {
        // Test formatting
        assert(validator.formatPhoneNumber("9876543210") != null)
        val formatted = validator.formatPhoneNumber("9876543210")
        assert(formatted == "+91 98765 43210")
    }

    @Test
    fun `formatPhoneNumber should return null for invalid phone numbers`() {
        // Test invalid formatting
        assert(validator.formatPhoneNumber("987654321") == null) // Too short
        assert(validator.formatPhoneNumber("98765432101") == null) // Too long
        assert(validator.formatPhoneNumber("5876543210") == null) // Invalid prefix
        assert(validator.formatPhoneNumber("abcdefghij") == null) // Non-numeric
    }
}
