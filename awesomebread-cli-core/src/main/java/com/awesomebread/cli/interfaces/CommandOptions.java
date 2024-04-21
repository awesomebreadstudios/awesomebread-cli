package com.awesomebread.cli.interfaces;

import com.awesomebread.cli.annotations.Flag;
import com.awesomebread.cli.exceptions.InvalidOptionsException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.val;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.awesomebread.cli.util.CommandLineCollector.splitBySeparator;

public interface CommandOptions {

    String FLAG = "-";

    String VERBOSE_FLAG = "--";

    Map<Class<?>, Function<String, Object>> SUPPORTED_TYPES = Map.of(
            String.class, string -> string,
            Integer.class, Integer::parseInt,
            int.class, Integer::parseInt,
            Boolean.class, Boolean::parseBoolean,
            boolean.class, Boolean::parseBoolean
    );

    default void parseArgsToOptions(final List<String> commandArgs) {
        val topLevelArgs = new ArrayList<String>();
        val separatedArgs = commandArgs.stream()
                .collect(splitBySeparator(str -> str.startsWith(FLAG) || str.startsWith(VERBOSE_FLAG), true));

        separatedArgs.forEach(argList -> {
            if (argList.get(0).startsWith(FLAG) || argList.get(0).startsWith(VERBOSE_FLAG)) {
                val command = argList.get(0).replace(FLAG, "").replace(VERBOSE_FLAG, "");
                var field = FieldUtils.getDeclaredField(this.getClass(), command, true);
                if (field == null) {
                    val flagFields = FieldUtils.getFieldsListWithAnnotation(this.getClass(), Flag.class);
                    field = flagFields.stream()
                            .filter(f -> command.equals(f.getAnnotation(Flag.class).value()))
                            .findFirst()
                            .orElseThrow(() -> new InvalidOptionsException(String.format("Unknown option %s passed in", command)));
                }

                val argsToApply = argList.subList(1, argList.size());

                if (boolean.class.equals(field.getType()) || Boolean.class.equals(field.getType())) {
                    assignObjectField(field, this, true);
                    topLevelArgs.addAll(argsToApply);
                } else {
                    try {
                        val object = field.getType().getConstructor().newInstance();
                        topLevelArgs.addAll(applyArgsToFields(object, argsToApply));
                        assignObjectField(field, this, object);
                    } catch (NoSuchMethodException e) {
                        throw new InvalidOptionsException("Could not initialize options object", e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                topLevelArgs.addAll(argList);
            }
        });

        applyArgsToFields(this, topLevelArgs);
    }

    default List<String> applyArgsToFields(final Object object, final List<String> args) {
        val leftOverArgs = new ArrayList<String>();
        val fields = Arrays.stream(object.getClass().getDeclaredFields())
                .filter(field -> !(Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers())))
                .filter(field -> SUPPORTED_TYPES.containsKey(field.getType()) &&
                        !boolean.class.equals(field.getType()) &&
                        !Boolean.class.equals(field.getType()))
                .toList();

        IntStream.range(0, args.size())
                .forEach(i -> {
                    if (i < fields.size()) {
                        val arg = args.get(i);
                        val field = fields.get(i);
                        field.setAccessible(true);
                        assignObjectField(fields.get(i), object, SUPPORTED_TYPES.get(arg.getClass()).apply(arg));
                    }
                });

        return leftOverArgs;
    }

    default void assignObjectField(final Field field, final Object objectToSet, final Object value) {
        try {
            field.set(objectToSet, value);
        } catch (IllegalAccessException e) {
            throw new InvalidOptionsException("Could not assign field", e);
        }
    }

    default String help() {
        val helpStringBuilder = new StringBuilder();
        Arrays.stream(this.getClass().getDeclaredFields())
                .filter(field -> !(Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers())))
                .forEach(field -> {
                    helpStringBuilder.append("                ");
                    val flagAnno = field.getAnnotation(Flag.class);

                    if (flagAnno != null) {
                        helpStringBuilder
                                .append("--")
                                .append(field.getName())
                                .append(", -")
                                .append(flagAnno.value());
                    } else {
                        helpStringBuilder.append(field.getName());
                    }

                    helpStringBuilder.append(" - ").append(field.getType().getSimpleName());

                    val notNull = field.getAnnotation(NotNull.class);
                    val notEmpty = field.getAnnotation(NotEmpty.class);
                    val notBlank = field.getAnnotation(NotBlank.class);

                    if (notNull != null || notEmpty != null || notBlank != null) {
                        helpStringBuilder.append(" - REQUIRED");
                    }

                    helpStringBuilder.append("\n");

                    if (
                        flagAnno != null &&
                        !boolean.class.equals(field.getType()) &&
                        !Boolean.class.equals(field.getType())
                    ) {
                        Arrays.stream(field.getType().getDeclaredFields())
                                .filter(nestedField -> !(Modifier.isFinal(nestedField.getModifiers()) && Modifier.isStatic(nestedField.getModifiers())))
                                .forEach(nestedField -> {
                                    helpStringBuilder
                                            .append(nestedField.getName())
                                            .append(" - ")
                                            .append(field.getType().getSimpleName());

                                    val nestedNotNull = nestedField.getAnnotation(NotNull.class);
                                    val nestedNotEmpty = nestedField.getAnnotation(NotEmpty.class);
                                    val nestedNotBlank = nestedField.getAnnotation(NotBlank.class);

                                    if (nestedNotNull != null || nestedNotEmpty != null || nestedNotBlank != null) {
                                        helpStringBuilder.append(" - REQUIRED");
                                    }

                                    helpStringBuilder.append("\n");
                                });
                    }
                });

        return helpStringBuilder.toString();
    }
}
