package com.artificialsolutions.common;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Class containing different useful static methods to handle JSON.
 */
public class JsonUtils {

    /**
     * Gets the number of characters needed to represent a nonnegative integer number as a string in
     * binary format without leading zeros.
     * 
     * @param n the nonnegative integer number.
     * 
     * @return the number of characters needed to represent a the given value.
     */
    public static int getBinaryStringLengthForNonnegativeInteger(int n) {
        int k = 0;
        do {
            k++;
        } while ((n >>= 1) > 0);
        return k;
    }

    /**
     * Gets the number of characters needed to represent a nonnegative integer number as a string in
     * hexadecimal format without any prefixes like "0x" or similar and without leading zeros. For
     * instance, the value of 10 will need one character ("A"), the value of 256 will need three
     * characters ("100") etc.
     * 
     * @param n the nonnegative integer number.
     * 
     * @return the number of characters needed to represent a the given value.
     */
    public static int getHexStringLengthForNonnegativeInteger(int n) {
        int k = 0;
        do {
            k++;
        } while ((n >>= 4) > 0);
        return k;
    }

    /**
     * Appends a nonnegative integer number as a hexadecimal string without any prefixes (like "0x" or
     * similar) padding it with initial zeros to achieve the specified minimal numbers of appended
     * characters.
     * 
     * @param ap the {@code Appendable} object to append the result to.
     * @param n the nonnegative integer number.
     * @param minLength the minimal number of characters to append.
     * 
     * @return the {@code Appendable} referenced by {@code ap}.
     * 
     * @throws IOException if the appending failed.
     */
    public static Appendable appendNonnegativeHexInteger(final Appendable ap, final int n, final int minLength) throws IOException {
        int k = getHexStringLengthForNonnegativeInteger(n);
        for (int j = minLength; j > k; j--) ap.append('0');
        do {
            switch ((n >> --k * 4) & 0b1111) {
                case 15:
                    ap.append('f');
                    break;
                case 14:
                    ap.append('e');
                    break;
                case 13:
                    ap.append('d');
                    break;
                case 12:
                    ap.append('c');
                    break;
                case 11:
                    ap.append('b');
                    break;
                case 10:
                    ap.append('a');
                    break;
                case 9:
                    ap.append('9');
                    break;
                case 8:
                    ap.append('8');
                    break;
                case 7:
                    ap.append('7');
                    break;
                case 6:
                    ap.append('6');
                    break;
                case 5:
                    ap.append('5');
                    break;
                case 4:
                    ap.append('4');
                    break;
                case 3:
                    ap.append('3');
                    break;
                case 2:
                    ap.append('2');
                    break;
                case 1:
                    ap.append('1');
                    break;
                default:
                    ap.append('0');
            }
        } while (k > 0);
        return ap;
    }

    /**
     * Appends a nonnegative integer number as a hexadecimal string without any prefixes (like "0x" or
     * similar) padding it with initial zeros to achieve the specified minimal numbers of appended
     * characters. This method works the same as
     * {@link #appendNonnegativeHexInteger(Appendable, int, int)}.
     * 
     * @param sb the {@code StringBuilder} object to append the result to.
     * @param n the nonnegative integer number.
     * @param minLength the minimal number of characters to append.
     * 
     * @return the {@code StringBuilder} referenced by {@code sb}.
     */
    public static StringBuilder appendNonnegativeHexInteger(final StringBuilder sb, final int n, final int minLength) {
        try {
            appendNonnegativeHexInteger((Appendable)sb, n, minLength);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends a nonnegative integer number as a hexadecimal string without any prefixes (like "0x" or
     * similar) padding it with initial zeros to achieve the specified minimal numbers of appended
     * characters. This method works the same as
     * {@link #appendNonnegativeHexInteger(Appendable, int, int)}.
     * 
     * @param sb the {@code StringBuffer} object to append the result to.
     * @param n the nonnegative integer number.
     * @param minLength the minimal number of characters to append.
     * 
     * @return the {@code StringBuffer} referenced by {@code sb}.
     */
    public static StringBuffer appendNonnegativeHexInteger(final StringBuffer sb, final int n, final int minLength) {
        try {
            appendNonnegativeHexInteger((Appendable)sb, n, minLength);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * An auxiliary method to check if the given character is a whitespace, an ISO control or its code
     * is less than the code of a blank.
     * 
     * @param c the character to check.
     * 
     * @return {@code true} if the given character is a whitespace, an ISO control or its code is less
     * than the code of a blank. {@code false} otherwise.
     */
    private static boolean isAuxSpecial(final char c) {
        return c < ' ' || Character.isISOControl(c) || Character.isWhitespace(c);
    }

    /**
     * Appends a character while encoding/escaping it for JSON.
     * 
     * @param ap the {@code Appendable} object to append the result to.
     * @param c the char to append.
     * 
     * @return the {@code Appendable} referenced by {@code ap}.
     * 
     * @throws IOException if the appending failed.
     */
    public static Appendable append(final Appendable ap, final char c) throws IOException {
        switch (c) {
            case ' ':
                return ap.append(' ');
            case '/':
                return ap.append("\\/");
            case '"':
                return ap.append("\\\"");
            case '\n':
                return ap.append("\\n");
            case '\r':
                return ap.append("\\r");
            case '\\':
                return ap.append("\\\\");
            case '\t':
                return ap.append("\\t");
            case '\b':
                return ap.append("\\b");
            case '\f':
                return ap.append("\\f");
        }
        return isAuxSpecial(c) ? appendNonnegativeHexInteger(ap.append("\\u"), c, 4) : ap.append(c);
    }

    /**
     * Appends a character while encoding/escaping it for JSON. This method works the same as
     * {@link #append(Appendable, char)}.
     * 
     * @param sb the {@code StringBuilder} object to append the result to.
     * @param c the char to append.
     * 
     * @return the {@code StringBuilder} referenced by {@code sb}.
     */
    public static StringBuilder append(final StringBuilder sb, final char c) {
        try {
            append((Appendable)sb, c);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends a character while encoding/escaping it for JSON. This method works the same as
     * {@link #append(Appendable, char)}.
     * 
     * @param sb the {@code StringBuffer} object to append the result to.
     * @param c the char to append.
     * 
     * @return the {@code StringBuffer} referenced by {@code sb}.
     */
    public static StringBuffer append(final StringBuffer sb, final char c) {
        try {
            append((Appendable)sb, c);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Gets the number of the characters to be appended with {@link #append(Appendable, char)},
     * {@link #append(StringBuilder, char)} or {@link #append(StringBuffer, char)}.
     * 
     * @param c the character.
     * 
     * @return the number of the characters to be appended.
     */
    public static int getAppendedLength(final char c) {
        switch (c) {
            case ' ':
                return 1;
            case '/':
            case '"':
            case '\n':
            case '\r':
            case '\\':
            case '\t':
            case '\b':
            case '\f':
                return 2;
        }
        return isAuxSpecial(c) ? 6 : 1;
    }

    /**
     * Appends a char sequence while encoding/escaping it for JSON.
     * 
     * @param ap the {@code Appendable} object to append the result to.
     * @param s the char sequence to append.
     * 
     * @return the {@code Appendable} referenced by {@code ap}.
     * 
     * @throws IOException if the appending failed.
     */
    public static Appendable append(final Appendable ap, final CharSequence s) throws IOException {
        final int ln = s.length();
        for (int i = 0; i < ln; i++) append(ap, s.charAt(i));
        return ap;
    }

    /**
     * Appends a char sequence while encoding/escaping it for JSON. This method works the same as
     * {@link #append(Appendable, CharSequence)}.
     * 
     * @param sb the {@code StringBuilder} object to append the result to.
     * @param s the char sequence to append.
     * 
     * @return the {@code StringBuilder} referenced by {@code sb}.
     */
    public static StringBuilder append(final StringBuilder sb, final CharSequence s) {
        try {
            append((Appendable)sb, s);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends a char sequence while encoding/escaping it for JSON. This method works the same as
     * {@link #append(Appendable, CharSequence)}.
     * 
     * @param sb the {@code StringBuffer} object to append the result to.
     * @param s the char sequence to append.
     * 
     * @return the {@code StringBuffer} referenced by {@code sb}.
     */
    public static StringBuffer append(final StringBuffer sb, final CharSequence s) {
        try {
            append((Appendable)sb, s);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Gets the number of the characters to be appended with {@link #append(Appendable, CharSequence)},
     * {@link #append(StringBuilder, CharSequence)} or {@link #append(StringBuffer, CharSequence)}.
     * 
     * @param s the char sequence.
     * 
     * @return the number of the characters to be appended.
     */
    public static int getAppendedLength(final CharSequence s) {
        final int ln = s.length();
        int k = 0;
        for (int i = 0; i < ln; i++) k += getAppendedLength(s.charAt(i));
        return k;
    }

    /**
     * Appends a char sequence as a JSON string.
     * 
     * @param ap the {@code Appendable} object to append the result to.
     * @param s the char sequence to append.
     * 
     * @return the {@code Appendable} referenced by {@code ap}.
     * 
     * @throws IOException if the appending failed.
     */
    public static Appendable appendAsJson(final Appendable ap, final CharSequence s) throws IOException {
        return append(ap.append('"'), s).append('"');
    }

    /**
     * Appends a char sequence as a JSON string. This method works the same as
     * {@link #appendAsJson(Appendable, CharSequence)}.
     * 
     * @param sb the {@code StringBuilder} object to append the result to.
     * @param s the char sequence to append.
     * 
     * @return the {@code StringBuilder} referenced by {@code sb}.
     */
    public static StringBuilder appendAsJson(final StringBuilder sb, final CharSequence s) {
        try {
            appendAsJson((Appendable)sb, s);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends a char sequence as a JSON string. This method works the same as
     * {@link #appendAsJson(Appendable, CharSequence)}.
     * 
     * @param sb the {@code StringBuffer} object to append the result to.
     * @param s the char sequence to append.
     * 
     * @return the {@code StringBuffer} referenced by {@code sb}.
     */
    public static StringBuffer appendAsJson(final StringBuffer sb, final CharSequence s) {
        try {
            appendAsJson((Appendable)sb, s);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends a character as a JSON string.
     * 
     * @param ap the {@code Appendable} object to append the result to.
     * @param c the character to append.
     * 
     * @return the {@code Appendable} referenced by {@code ap}.
     * 
     * @throws IOException if the appending failed.
     */
    public static Appendable appendAsJson(final Appendable ap, final char c) throws IOException {
        return append(ap.append('"'), c).append('"');
    }

    /**
     * Appends a character as a JSON string. This method works the same as
     * {@link #appendAsJson(Appendable, char)}.
     * 
     * @param sb the {@code StringBuilder} object to append the result to.
     * @param c the character to append.
     * 
     * @return the {@code StringBuilder} referenced by {@code sb}.
     */
    public static StringBuilder appendAsJson(final StringBuilder sb, final char c) {
        try {
            appendAsJson((Appendable)sb, c);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends a character as a JSON string. This method works the same as
     * {@link #appendAsJson(Appendable, char)}.
     * 
     * @param sb the {@code StringBuffer} object to append the result to.
     * @param c the character to append.
     * 
     * @return the {@code StringBuffer} referenced by {@code sb}.
     */
    public static StringBuffer appendAsJson(final StringBuffer sb, final char c) {
        try {
            appendAsJson((Appendable)sb, c);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends a {@code Map} as a JSON string. The keys of the {@code Map} instance should one of the
     * following types: {@link java.lang.CharSequence}, {@link java.lang.Number} or
     * {@link java.lang.Enum}. In the case of {@code Number} the {@link java.lang.Number#toString()}
     * will be used to produce the string key. In the case of {@code Enum}, the key will be produced by
     * calling {@link java.lang.Enum#name()}
     * 
     * @param ap the {@code Appendable} object to append the result to.
     * @param m the {@code Map} to append.
     * 
     * @return the {@code Appendable} referenced by {@code ap}.
     * 
     * @throws IOException if the appending failed.
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static Appendable appendAsJson(final Appendable ap, final Map<?, ?> m) throws IllegalArgumentException, IOException {
        boolean added = false;
        ap.append('{');
        for (final Map.Entry<?, ?> e : m.entrySet()) {
            if (added) ap.append(',');
            else added = true;
            final Object key = e.getKey();
            if (key instanceof CharSequence) appendAsJson(ap, (CharSequence)key);
            else if (key instanceof Enum) appendAsJson(ap, ((Enum<?>)key).name());
            else if (key instanceof Number) ap.append('"').append(key.toString()).append('"');
            else if (key == null) throw new IllegalArgumentException("Key may only be a CharSequence (String), enum or number (to be converted to String), is null");
            else throw new IllegalArgumentException("Key may only be a CharSequence (String), enum or number (to be converted to String), is " + key.getClass());
            appendAsJson(ap.append(':'), e.getValue());
        }
        return ap.append('}');
    }

    /**
     * Appends a {@code Map} as a JSON string. This method works the same as
     * {@link #appendAsJson(Appendable, Map)}.
     * 
     * @param sb the {@code StringBuilder} object to append the result to.
     * @param m the {@code Map} to append.
     * 
     * @return the {@code StringBuilder} referenced by {@code sb}.
     * 
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static StringBuilder appendAsJson(final StringBuilder sb, final Map<?, ?> m) throws IllegalArgumentException {
        try {
            appendAsJson((Appendable)sb, m);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends a {@code Map} as a JSON string. This method works the same as
     * {@link #appendAsJson(Appendable, Map)}.
     * 
     * @param sb the {@code StringBuffer} object to append the result to.
     * @param m the {@code Map} to append.
     * 
     * @return the {@code StringBuffer} referenced by {@code sb}.
     * 
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static StringBuffer appendAsJson(final StringBuffer sb, final Map<?, ?> m) throws IllegalArgumentException {
        try {
            appendAsJson((Appendable)sb, m);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends a {@code Collection} as a JSON string.
     * 
     * @param ap the {@code Appendable} object to append the result to.
     * @param xx the {@code Collection} to append.
     * 
     * @return the {@code Appendable} referenced by {@code ap}.
     * 
     * @throws IOException if the appending failed.
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static Appendable appendAsJson(final Appendable ap, final Collection<?> xx) throws IllegalArgumentException, IOException {
        boolean added = false;
        ap.append('[');
        for (final Object x : xx) {
            if (added) ap.append(',');
            else added = true;
            appendAsJson(ap, x);
        }
        return ap.append(']');
    }

    /**
     * Appends a {@code Collection} as a JSON string. This method works the same as
     * {@link #appendAsJson(Appendable, Collection)}.
     * 
     * @param sb the {@code StringBuilder} object to append the result to.
     * @param xx the {@code Collection} to append.
     * 
     * @return the {@code StringBuilder} referenced by {@code sb}.
     * 
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static StringBuilder appendAsJson(final StringBuilder sb, final Collection<?> xx) throws IllegalArgumentException {
        try {
            appendAsJson((Appendable)sb, xx);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends a {@code Collection} as a JSON string. This method works the same as
     * {@link #appendAsJson(Appendable, Collection)}.
     * 
     * @param sb the {@code StringBuffer} object to append the result to.
     * @param xx the {@code Collection} to append.
     * 
     * @return the {@code StringBuffer} referenced by {@code sb}.
     * 
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static StringBuffer appendAsJson(final StringBuffer sb, final Collection<?> xx) throws IllegalArgumentException {
        try {
            appendAsJson((Appendable)sb, xx);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends an array as a JSON string.
     * 
     * @param ap the {@code Appendable} object to append the result to.
     * @param xx the array to append.
     * 
     * @return the {@code Appendable} referenced by {@code ap}.
     * 
     * @throws IOException if the appending failed.
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static Appendable appendAsJson(final Appendable ap, final Object[] xx) throws IllegalArgumentException, IOException {
        ap.append('[');
        for (int i = 0; i < xx.length; i++) {
            if (i > 0) ap.append(',');
            appendAsJson(ap, xx[i]);
        }
        return ap.append(']');
    }

    /**
     * Appends an array as a JSON string. This method works the same as
     * {@link #appendAsJson(Appendable, Object[])}.
     * 
     * @param sb the {@code StringBuilder} object to append the result to.
     * @param xx the array to append.
     * 
     * @return the {@code StringBuilder} referenced by {@code sb}.
     * 
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static StringBuilder appendAsJson(final StringBuilder sb, final Object[] xx) throws IllegalArgumentException {
        try {
            appendAsJson((Appendable)sb, xx);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends an array as a JSON string. This method works the same as
     * {@link #appendAsJson(Appendable, Object[])}.
     * 
     * @param sb the {@code StringBuffer} object to append the result to.
     * @param xx the array to append.
     * 
     * @return the {@code StringBuffer} referenced by {@code sb}.
     * 
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static StringBuffer appendAsJson(final StringBuffer sb, final Object[] xx) throws IllegalArgumentException {
        try {
            appendAsJson((Appendable)sb, xx);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends an object as a JSON string.
     * 
     * @param ap the {@code Appendable} object to append the result to.
     * @param x the object to append as JSON.
     * 
     * @return the {@code Appendable} referenced by {@code ap}.
     * 
     * @throws IOException if the appending failed.
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static Appendable appendAsJson(final Appendable ap, final Object x) throws IllegalArgumentException, IOException {
        if (x == null) return ap.append("null");
        if (x instanceof Boolean) return ap.append(x.toString());
        if (x instanceof CharSequence) return appendAsJson(ap, (CharSequence)x);
        if (x instanceof Character) return appendAsJson(ap, ((Character)x).charValue());
        if (x instanceof Number) return ap.append(x.toString());
        if (x instanceof Map) return appendAsJson(ap, (Map<?, ?>)x);
        if (x instanceof Collection) return appendAsJson(ap, (Collection<?>)x);
        if (x instanceof Object[]) return appendAsJson(ap, (Object[])x);
        if (x instanceof Enum) return appendAsJson(ap, ((Enum<?>)x).name());
        return appendAsJson(ap, x.toString());
    }

    /**
     * Appends an object as a JSON string. This method works the same as
     * {@link #appendAsJson(Appendable, Object)}.
     * 
     * @param sb the {@code StringBuilder} object to append the result to.
     * @param x the object to append as JSON.
     * 
     * @return the {@code StringBuilder} referenced by {@code sb}.
     * 
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static StringBuilder appendAsJson(final StringBuilder sb, final Object x) throws IllegalArgumentException {
        try {
            appendAsJson((Appendable)sb, x);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Appends an object as a JSON string. This method works the same as
     * {@link #appendAsJson(Appendable, Object)}.
     * 
     * @param sb the {@code StringBuffer} object to append the result to.
     * @param x the object to append as JSON.
     * 
     * @return the {@code StringBuffer} referenced by {@code sb}.
     * 
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static StringBuffer appendAsJson(final StringBuffer sb, final Object x) throws IllegalArgumentException {
        try {
            appendAsJson((Appendable)sb, x);
        } catch (final IOException e) {
            // Cannot happen
        }
        return sb;
    }

    /**
     * Converts an object to a JSON structure.
     * 
     * @param x the object to return as JSON.
     * 
     * @return the object as a JSON structure.
     * 
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static String toStringifiedJson(final Object x) throws IllegalArgumentException {
        return appendAsJson(new StringBuilder(), x).toString();
    }

    /**
     * Converts a {@code Map} to a JSON structure.
     * 
     * @param m the {@code Map} to return as JSON.
     * 
     * @return the {@code Map} as a JSON structure.
     * 
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static String toStringifiedJson(final Map<?, ?> m) throws IllegalArgumentException {
        return appendAsJson(new StringBuilder(), m).toString();
    }

    /**
     * Converts a {@code Collection} to a JSON structure.
     * 
     * @param xx the {@code Collection} to return as JSON.
     * 
     * @return the {@code Collection} as a JSON structure.
     * 
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static String toStringifiedJson(final Collection<?> xx) throws IllegalArgumentException {
        return appendAsJson(new StringBuilder(), xx).toString();
    }

    /**
     * Converts an array to a JSON structure.
     * 
     * @param xx the array to return as JSON.
     * 
     * @return the array as a JSON structure.
     * 
     * @throws IllegalArgumentException when a null map key is encountered.
     */
    public static String toStringifiedJson(final Object[] xx) throws IllegalArgumentException {
        return appendAsJson(new StringBuilder(), xx).toString();
    }
}
