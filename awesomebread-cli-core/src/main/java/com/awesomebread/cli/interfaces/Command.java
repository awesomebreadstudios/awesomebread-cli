package com.awesomebread.cli.interfaces;

import com.awesomebread.cli.annotations.AwesomebreadCommand;
import com.google.common.reflect.TypeToken;
import lombok.val;

import java.lang.reflect.InvocationTargetException;

public interface Command<T> {
    default void execute(Object options) {
        run(getOptionsClass().cast(options));
    }

    void run(T options);

    @SuppressWarnings("unchecked")
    default Class<T> getOptionsClass() {
        TypeToken<T> typeToken = new TypeToken<T>(getClass()) {};
        return (Class<T>) typeToken.getRawType();
    }

    default String help() {
        val helpStringBuilder = new StringBuilder();
        helpStringBuilder
                .append(String.format(
                        "Command:        %s",
                        this.getClass().getAnnotation(AwesomebreadCommand.class).value()));

        val optionsClass = getOptionsClass();
        if (!Void.class.equals(optionsClass)) {
            T temp = null;
            try {
                temp = optionsClass.getConstructor().newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            helpStringBuilder
                    .append("\nOptions:\n")
                    .append(((CommandOptions) temp).help());
        }

        return helpStringBuilder.toString();
    }
}
