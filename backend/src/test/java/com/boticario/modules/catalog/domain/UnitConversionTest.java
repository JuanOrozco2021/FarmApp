package com.boticario.modules.catalog.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cubre exactamente el ejemplo del encargo: Ibuprofeno 400 mg con unidad
 * base PASTILLA y presentaciones PASTILLA (factor 1), TABLETA (factor 10)
 * y CAJA (factor 100).
 */
class UnitConversionTest {

    private final Unit pastilla = unit("PASTILLA");
    private final Unit tableta = unit("TABLETA");
    private final Unit caja = unit("CAJA");

    @Test
    void unaPastillaEquivaleAUnaUnidadBase() {
        ProductPresentation presentacionPastilla = presentation(pastilla, "1");

        assertThat(UnitConversion.toBaseUnits(presentacionPastilla, new BigDecimal("1")))
                .isEqualByComparingTo("1");
    }

    @Test
    void unaTabletaEquivaleADiezPastillas() {
        ProductPresentation presentacionTableta = presentation(tableta, "10");

        assertThat(UnitConversion.toBaseUnits(presentacionTableta, new BigDecimal("1")))
                .isEqualByComparingTo("10");
    }

    @Test
    void unaCajaEquivaleACienPastillas() {
        ProductPresentation presentacionCaja = presentation(caja, "100");

        assertThat(UnitConversion.toBaseUnits(presentacionCaja, new BigDecimal("1")))
                .isEqualByComparingTo("100");
    }

    @Test
    void cincoTabletasEquivalenACincuentaPastillas() {
        ProductPresentation presentacionTableta = presentation(tableta, "10");

        assertThat(UnitConversion.toBaseUnits(presentacionTableta, new BigDecimal("5")))
                .isEqualByComparingTo("50");
    }

    @Test
    void diezPastillasBaseEquivalenAUnaTableta() {
        ProductPresentation presentacionTableta = presentation(tableta, "10");

        assertThat(UnitConversion.fromBaseUnits(presentacionTableta, new BigDecimal("10")))
                .isEqualByComparingTo("1");
    }

    @Test
    void rechazaCantidadesEnCeroONegativas() {
        ProductPresentation presentacionPastilla = presentation(pastilla, "1");

        assertThatThrownBy(() -> UnitConversion.toBaseUnits(presentacionPastilla, BigDecimal.ZERO))
                .isInstanceOf(InvalidQuantityException.class);
        assertThatThrownBy(() -> UnitConversion.toBaseUnits(presentacionPastilla, new BigDecimal("-1")))
                .isInstanceOf(InvalidQuantityException.class);
    }

    private static Unit unit(String code) {
        Unit unit = new Unit();
        unit.setCode(code);
        unit.setName(code);
        return unit;
    }

    private static ProductPresentation presentation(Unit unit, String factor) {
        ProductPresentation presentation = new ProductPresentation();
        presentation.setUnit(unit);
        presentation.setConversionFactor(new BigDecimal(factor));
        return presentation;
    }
}
