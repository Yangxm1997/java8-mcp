package top.yangxm.ai.mcp.org.springframework.ai.util;

import com.fasterxml.jackson.databind.Module;
import org.springframework.beans.BeanUtils;
import org.springframework.core.KotlinDetector;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

public final class JacksonUtils {
    private JacksonUtils() {
    }

    @SuppressWarnings("unchecked")
    public static List<Module> instantiateAvailableModules() {
        List<Module> modules = new ArrayList<>();
        try {
            Class<? extends Module> jdk8ModuleClass = (Class<? extends Module>) ClassUtils
                    .forName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module", null);
            Module jdk8Module = BeanUtils.instantiateClass(jdk8ModuleClass);
            modules.add(jdk8Module);
        } catch (ClassNotFoundException ex) {
            // jackson-datatype-jdk8 not available
        }

        try {
            Class<? extends Module> javaTimeModuleClass = (Class<? extends Module>) ClassUtils
                    .forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", null);
            Module javaTimeModule = BeanUtils.instantiateClass(javaTimeModuleClass);
            modules.add(javaTimeModule);
        } catch (ClassNotFoundException ex) {
            // jackson-datatype-jsr310 not available
        }

        try {
            Class<? extends Module> parameterNamesModuleClass = (Class<? extends Module>) ClassUtils
                    .forName("com.fasterxml.jackson.module.paramnames.ParameterNamesModule", null);
            Module parameterNamesModule = BeanUtils
                    .instantiateClass(parameterNamesModuleClass);
            modules.add(parameterNamesModule);
        } catch (ClassNotFoundException ex) {
            // jackson-module-parameter-names not available
        }

        // Kotlin present?
        if (KotlinDetector.isKotlinPresent()) {
            try {
                Class<? extends Module> kotlinModuleClass = (Class<? extends Module>) ClassUtils
                        .forName("com.fasterxml.jackson.module.kotlin.KotlinModule", null);
                Module kotlinModule = BeanUtils.instantiateClass(kotlinModuleClass);
                modules.add(kotlinModule);
            } catch (ClassNotFoundException ex) {
                // jackson-module-kotlin not available
            }
        }
        return modules;
    }
}
