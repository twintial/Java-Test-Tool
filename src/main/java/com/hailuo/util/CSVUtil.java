package com.hailuo.util;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVUtil {

    public static final char SEPARATOR = ',';

    public static String[] getHeaders(String filePath) {
        CsvReader reader = null;

        try{
            reader = new CsvReader(filePath, SEPARATOR, StandardCharsets.UTF_8);
            reader.setSafetySwitch(false);
            reader.readHeaders();
            return reader.getHeaders();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return null;
    }

    public static List<String[]> getContents(String filePath) {
        CsvReader reader = null;
        List<String[]> rows = new ArrayList<>();
        try{
            reader = new CsvReader(filePath, SEPARATOR, StandardCharsets.UTF_8);
            reader.setSafetySwitch(false);
            reader.readHeaders();
            while (reader.readRecord()) {
                rows.add(reader.getValues());
            }
            return rows;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return null;
    }

    public static void writeContents(String filePath, List<List<String>> contents){
        CsvWriter writer = new CsvWriter(filePath, SEPARATOR, StandardCharsets.UTF_8);
        try {
            for (List<String> content : contents) {
                writer.writeRecord(content.toArray(new String[0]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writer.close();
        }
    }
}
