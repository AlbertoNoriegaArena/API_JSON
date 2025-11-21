package com.ejemploAPI.config;

import com.ejemploAPI.models.AttributeType;
import com.ejemploAPI.services.AttributeTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class StartupDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupDataLoader.class);

    private final AttributeTypeService attributeTypeService;

    public StartupDataLoader(AttributeTypeService attributeTypeService) {
        this.attributeTypeService = attributeTypeService;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // Precargar "día" enum con días de la semana
            AttributeType dia = attributeTypeService.ensureEnumType("día");
            List<String> dias = Arrays.asList("LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO", "DOMINGO");
            attributeTypeService.addValuesToAttributeType(dia, dias);

            // Precargar "color" enum con colores básicos
            AttributeType color = attributeTypeService.ensureEnumType("color");
            List<String> colores = Arrays.asList("ROJO", "AZUL", "AMARILLO", "VERDE");
            attributeTypeService.addValuesToAttributeType(color, colores);

            // Precargar "mes" enum con el nombre de los meses
            AttributeType mes = attributeTypeService.ensureEnumType("mes");
            List<String> meses = Arrays.asList("ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE");
            attributeTypeService.addValuesToAttributeType(mes, meses);

            log.info("StartupDataLoader: enums precargados: 'día' y 'color'");
        } catch (Exception ex) {
            log.error("Error precargando enums en arranque", ex);
        }
    }
}
