package com.company;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class Main {

    public static void main(String[] args) {
        String gzipFile = "https://github.com/PeacockTeam/new-job/releases/download/v1.0/lng-4.txt.gz";
        String newFileTxt = "out.txt";
        if (args.length > 0) newFileTxt = args[0];
        decompressGzipFileNew(gzipFile, newFileTxt);
    }

    private static void decompressGzipFileNew(String gzipFile, String newFile) {
//        ArrayList<String> list = new ArrayList<>();
//        StringBuilder stringBuilder1 = new StringBuilder().append("\"858\";").append("\"56\";").append("\"45\";").append("\"74\";");
//        list.add(stringBuilder1.toString());
//        StringBuilder stringBuilder2 = new StringBuilder().append("\"85\";").append("\"10\";").append("\"45\";").append("\"74\";");
//        list.add(stringBuilder2.toString());

        try {
            InputStream fis = new URL(gzipFile).openStream();
            GZIPInputStream gis = new GZIPInputStream(fis);
            Stream<String> out = new BufferedReader(new InputStreamReader(gis)).lines();
            Set<String> lists = out.collect(Collectors.toSet());

            // храним результат в виде списка множеств для уникальности: [номер_группы -> [строки_группы]]
            List<Set<String>> groups = new ArrayList<>();
            // используем вспомогательный список хэш-таблиц: [позиция_слова -> { слово -> номер_группы }]
            List<Map<String, Integer>> parts = new ArrayList<>();


            lists.stream().filter(s -> !Pattern.compile("\\d+\"\\d+").matcher(s).find()).forEach(new Consumer<String>() {
                @Override
                public void accept(String line) {
                    String[] columns = getColumnsOf(line);
                    Integer groupNumber = null;
                    for (int i = 0; i < Math.min(parts.size(), columns.length); i++) {
                        Integer groupNumber2 = parts.get(i).get(columns[i]);
                        if (groupNumber2 != null) {
                            if (groupNumber == null) {
                                groupNumber = groupNumber2;
                            } else if (!Objects.equals(groupNumber, groupNumber2)) {
                                for (String line2 : groups.get(groupNumber2)) {
                                    //копирование строк из текущей группы строк в предыдущую
                                    groups.get(groupNumber).add(line2);
                                    //копирование колонок из текущей группы колонок в предыдущую
                                    apply(getColumnsOf(line2), groupNumber, parts);
                                }
                                //обнуление строк у текущей группы
                                groups.set(groupNumber2, new HashSet<>());
                            }
                        }
                    }
                    if (groupNumber == null) {
                        if (Arrays.stream(columns).anyMatch(s -> !s.isEmpty())) {
                            //новая группа строк
                            groups.add(new HashSet<>(List.of(line)));
                            //новая группа колонок
                            apply(columns, groups.size() - 1, parts);
                        }
                    } else {
                        //добавляем к сущетсвующей группе строк еще строку
                        groups.get(groupNumber).add(line);
                        //добавляем к сущетсвующей группе колонок еще колонки
                        apply(columns, groupNumber, parts);
                    }
                }
            });

            gis.close();

            FileOutputStream fos = new FileOutputStream(newFile);
            Integer i = 0;
            var listTrim = groups.stream().filter(lst -> lst.size() > 1).collect(Collectors.toList());
            int totalCount = listTrim.size();
            listTrim.stream().forEach(new Consumer<Set<String>>() {
//            groups.stream().filter(lst -> lst.size() > 1).forEach(new Consumer<Set<String>>() {
                @Override
                public void accept(Set<String> group) {
                    try {
                        fos.write(new StringBuilder("\nГруппа ").append(groups.indexOf(group)).append("\n").toString().getBytes());
                        for (String val : group) {
                            fos.write(new StringBuilder(val).append("\n").toString().getBytes());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            fos.close();
            System.out.println(String.format("Total count:%s", totalCount));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String[] getColumnsOf(String line) {
        return line.replaceAll("\"", "").split(";");
    }

    private static void apply(String[] newValues, int groupNumber, List<Map<String, Integer>> parts) {
        for (int i = 0; i < newValues.length; i++) {
            if (newValues[i].isEmpty()) {
                continue;
            }
            if (i < parts.size()) {
                parts.get(i).put(newValues[i], groupNumber);
            } else {
                HashMap<String, Integer> map = new HashMap<>();
                map.put(newValues[i], groupNumber);
                parts.add(map);
            }
        }
    }

}
