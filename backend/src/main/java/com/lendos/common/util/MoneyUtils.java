package com.lendos.common.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Utility class for all financial calculations.
 * ALWAYS use BigDecimal for money. Never use double or float.
 * Scale = 2 for display amounts, Scale = 8 for intermediate calculations.
 */
public final class MoneyUtils {

    public static final int DISPLAY_SCALE = 2;
    public static final int CALC_SCALE = 8;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final MathContext MATH_CONTEXT = new MathContext(15, ROUNDING_MODE);

    private MoneyUtils() {}

    public static BigDecimal round(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.setScale(DISPLAY_SCALE, ROUNDING_MODE);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return round(a.add(b));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return round(a.subtract(b));
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return round(a.multiply(b, MATH_CONTEXT));
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return a.divide(b, CALC_SCALE, ROUNDING_MODE);
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isZeroOrNegative(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
    }

    public static boolean isGreaterThan(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) > 0;
    }

    public static BigDecimal percentOf(BigDecimal rate, BigDecimal amount) {
        return round(amount.multiply(rate.divide(BigDecimal.valueOf(100), CALC_SCALE, ROUNDING_MODE)));
    }
}
