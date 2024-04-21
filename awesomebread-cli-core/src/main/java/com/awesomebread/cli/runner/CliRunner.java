package com.awesomebread.cli.runner;

import com.awesomebread.cli.annotations.AwesomebreadCliRunner;
import com.awesomebread.cli.exceptions.InvalidOptionsException;
import com.awesomebread.cli.interfaces.Command;
import com.awesomebread.cli.interfaces.CommandOptions;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static com.awesomebread.cli.constants.CommandInputs.COMMAND_NEXT;
import static com.awesomebread.cli.util.CommandLineCollector.splitBySeparator;

@Component
@Slf4j
public class CliRunner implements CommandLineRunner {

    private final ConfigurableApplicationContext context;

    @Qualifier("command")
    private final Map<String, Command<?>> commands;

    private final ValidatorFactory factory;

    private final Validator validator;

    public CliRunner(
            final ConfigurableApplicationContext context,
            final Map<String, Command<?>> commands
    ) {
        this.context = context;
        this.commands = commands;
        this.factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Override
    public void run(final String... args) {
        log.debug("Starting cli runner with args {}", Arrays.toString(args));
        if (Arrays.stream(args).anyMatch(arg -> arg.equals("-h") || arg.equals("--help"))) {
            help();
            System.exit(0);
        }

        //TODO - Support COMMAND_PIPE with internal memory holders for passing data
        val commandArgPairs = parseAndValidate(Arrays.stream(args)
                .collect(splitBySeparator(Predicate.isEqual(COMMAND_NEXT))));

        commandArgPairs.forEach((pair) -> pair.getLeft().execute(pair.getRight()));

        log.debug("Finished cli runner with args {}", Arrays.toString(args));
        context.close();
    }

    private List<Pair<Command<?>, Object>> parseAndValidate(List<List<String>> commandArgsLists) {
        val commandArgPairs = new ArrayList<Pair<Command<?>, Object>>();

        commandArgsLists.forEach((commandArgs) -> {
            try {
                val command = commands.get(commandArgs.get(0));
                commandArgPairs.add(new ImmutablePair<>(
                        command,
                        parseOptions(command, commandArgs.subList(1, commandArgs.size()))
                ));
            } catch (NullPointerException e) {
                log.error("Command {} not found.", commandArgs.get(0));
                help();
                System.exit(1);
            } catch (InvalidOptionsException e) {
                log.error("Invalid options were passed for command and options {} - {}", commandArgs, e.getMessage());
                help();
                System.exit(2);
            }
        });

        return commandArgPairs;
    }

    private void help() {
        val helpStringBuilder = new StringBuilder().append("\n\n");
        val beans = context.getBeansWithAnnotation(AwesomebreadCliRunner.class);
        beans.forEach((name, bean) -> {
            helpStringBuilder.append(bean.getClass().getAnnotation(AwesomebreadCliRunner.class).description().trim());
        });
        helpStringBuilder.append("\n\nUsage:\n");
        commands.forEach((key, value) -> helpStringBuilder.append("\n").append(value.help()));
        log.info(helpStringBuilder.toString());
    }

    @SuppressWarnings("unchecked")
    private <T extends CommandOptions> T parseOptions(Command<?> command, List<String> commandArgs) {
        log.debug("Parsing options for command {}", command);

        val commandOptionsClass = command.getOptionsClass();

        if (Void.class.equals(commandOptionsClass)) {
            if (!commandArgs.isEmpty()) {
                throw new InvalidOptionsException("Void options called with arguments");
            }

            return null;
        }

        if (!CommandOptions.class.isAssignableFrom(commandOptionsClass)) {
            throw new InvalidOptionsException(String.format("%s does not implement CommandOptions", commandOptionsClass));
        }

        T optionsInstance;

        try {
            val constructor = commandOptionsClass.getConstructor();
            optionsInstance = (T) constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new InvalidOptionsException(String.format("No default constructor found for %s", commandOptionsClass), e);
        } catch (InvocationTargetException e) {
            throw new InvalidOptionsException(String.format("Could not construct %s", commandOptionsClass), e);
        } catch (InstantiationException e) {
            throw new InvalidOptionsException(String.format("Could not instantiate %s", commandOptionsClass), e);
        } catch (IllegalAccessException e) {
            throw new InvalidOptionsException(String.format("Could not access constructor for %s", commandOptionsClass), e);
        }

        optionsInstance.parseArgsToOptions(commandArgs);
        if (isOptionsValid(optionsInstance)) {
            return optionsInstance;
        }

        throw new InvalidOptionsException(String.format("Unable to parse and validate options for command %s and args %s", command, commandArgs));
    }

    private <T> boolean isOptionsValid(T commandOptions) {
        Set<ConstraintViolation<T>> violations = validator.validate(commandOptions);
        return violations.isEmpty();
    }
}
