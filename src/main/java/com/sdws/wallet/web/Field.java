package com.sdws.wallet.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

/** Describes one input of the generic form page (templates/form.html). */
@Getter
@AllArgsConstructor
public class Field {
    private final String name;
    private final String label;
    private final String type;
    private final String value;
    private final boolean required;
    private final List<String> options;
    private final String step;

    public static Field text(String name, String label) { return new Field(name, label, "text", null, true, null, null); }
    public static Field text(String name, String label, String value, boolean required) { return new Field(name, label, "text", value, required, null, null); }
    public static Field email(String name, String label, String value) { return new Field(name, label, "email", value, false, null, null); }
    public static Field password(String name, String label, boolean required) { return new Field(name, label, "password", null, required, null, null); }
    public static Field amount(String name, String label) { return new Field(name, label, "number", null, true, null, "0.01"); }
    public static Field select(String name, String label, List<String> options) { return new Field(name, label, "text", null, true, options, null); }
}
