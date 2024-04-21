package com.awesomebread.cli.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collector;

public class CommandLineCollector {

    public static Collector<String, List<List<String>>, List<List<String>>> splitBySeparator(Predicate<String> separator) {
        return splitBySeparator(separator, false);
    }

    public static Collector<String, List<List<String>>, List<List<String>>> splitBySeparator(Predicate<String> separator, boolean includeSeparator) {
        return Collector.of(
                () -> new ArrayList<>(List.of(new ArrayList<>())),
                (list, element) -> {
                    if (separator.test(element)) {
                        list.add(new ArrayList<>());
                        if (includeSeparator) {
                            list.get(list.size() - 1).add(element);
                        }
                    } else {
                        list.get(list.size() - 1).add(element);
                    }
                },
                (list1, list2) -> {
                    list1.get(list1.size() - 1).addAll(list2.remove(0));
                    list1.addAll(list2);
                    return list1;
                }
        );
    }


}
