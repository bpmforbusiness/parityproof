import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parity runner: byte-for-byte field-level diff between the COBOL reference output and the
 * Java modernized output for every program listed in programs.txt, using each program's
 * copybook-derived layout. Writes result.json (overall + per-program + per-record/field) and
 * exits non-zero if any program is not at 100% parity.
 *
 * programs.txt: one program per line -> name,layout,cobol-output,java-output
 *
 * A missing/crashed/empty COBOL or Java output is a clean FAIL for that program, never an
 * uncaught exception. The run scripts delete outputs before each run, so a crash leaves them
 * absent and we never diff against a stale file from a previous run.
 */
public final class ParityRunner {

    static final class Field {
        String name; int start, len, scale;
        Field(String n, int s, int l, int sc) { name = n; start = s; len = l; scale = sc; }
    }

    public static void main(String[] args) throws Exception {
        String manifest = args.length > 0 ? args[0] : "programs.txt";
        String outPath = args.length > 1 ? args[1] : "result.json";

        List<Map<String, Object>> programs = new ArrayList<>();
        int allFields = 0, allPass = 0;
        boolean allOk = true;
        String topError = null;

        List<String[]> specs = new ArrayList<>();
        try {
            specs = loadManifest(manifest);
            if (specs.isEmpty()) topError = "no programs listed in " + manifest;
        } catch (IOException e) {
            topError = "cannot read " + manifest + ": " + e.getMessage();
        }

        for (String[] s : specs) {
            Map<String, Object> p = compare(s[0], s[1], s[2], s[3]);
            programs.add(p);
            allFields += (int) p.get("fieldsTotal");
            allPass += (int) p.get("fieldsPass");
            if (!p.get("pass").equals(Boolean.TRUE)) allOk = false;
            System.out.println(s[0] + ": parity = " + p.get("fieldsPassed") + " fields | "
                    + (p.get("pass").equals(Boolean.TRUE) ? "PASS" : "FAIL"
                       + (p.containsKey("error") ? " (" + p.get("error") + ")" : "")));
        }
        if (topError != null) { allOk = false; System.out.println("parity = FAIL (" + topError + ")"); }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("pass", allOk && topError == null);
        root.put("parityPercent", allFields == 0 ? 0 : Math.round(allPass * 10000.0 / allFields) / 100.0);
        root.put("fieldsPassed", allPass + "/" + allFields);
        if (topError != null) root.put("error", topError);
        root.put("programs", programs);
        try (FileWriter w = new FileWriter(outPath)) { w.write(toJson(root)); }

        System.out.println("overall parity = " + root.get("fieldsPassed") + " fields (" + root.get("parityPercent")
                + "%) | " + (root.get("pass").equals(Boolean.TRUE) ? "PASS" : "FAIL"));
        if (!root.get("pass").equals(Boolean.TRUE)) System.exit(1);
    }

    /** Diff one program. Never throws: any I/O problem becomes a FAIL with an error message. */
    static Map<String, Object> compare(String name, String layoutPath, String cobolPath, String javaPath) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", name);
        List<Field> fields;
        try { fields = loadFields(layoutPath); }
        catch (IOException | RuntimeException e) { return failed(p, null, null, 0, "layout unreadable (" + layoutPath + "): " + e); }

        List<String> cobol = null, java = null;
        String error = null;
        try { cobol = readOutput(cobolPath); }
        catch (IOException e) { error = "COBOL output missing/unreadable (" + cobolPath + ")"; }
        try { java = readOutput(javaPath); }
        catch (IOException e) { error = (error == null ? "" : error + "; ") + "Java output missing/unreadable (" + javaPath + ")"; }
        if (error == null && cobol.isEmpty()) error = "COBOL output is empty";
        if (error == null && java.isEmpty())  error = "Java output is empty";
        if (error != null) return failed(p, cobol, java, fields.size(), error);

        int total = 0, pass = 0;
        int rows = Math.min(cobol.size(), java.size());
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            String c = cobol.get(i), j = java.get(i);
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("record", i + 1);
            int rowPass = 0;
            List<Map<String, Object>> flds = new ArrayList<>();
            for (Field f : fields) {
                String cv = field(c, f), jv = field(j, f);
                boolean ok = cv.equals(jv);
                if (ok) rowPass++;
                Map<String, Object> ff = new LinkedHashMap<>();
                ff.put("name", f.name); ff.put("pass", ok);
                if (!ok) { ff.put("cobol", cv); ff.put("java", jv); }
                flds.add(ff);
            }
            rec.put("fields", flds);
            rec.put("rowPass", rowPass + "/" + fields.size());
            records.add(rec);
            total += fields.size();
            pass += rowPass;
        }
        boolean linesEqual = cobol.size() == java.size();
        p.put("cobolLines", cobol.size());
        p.put("javaLines", java.size());
        p.put("linesEqual", linesEqual);
        p.put("parityPercent", total == 0 ? 0 : Math.round(pass * 10000.0 / total) / 100.0);
        p.put("fieldsPassed", pass + "/" + total);
        p.put("fieldsPass", pass);
        p.put("fieldsTotal", total);
        p.put("records", records);
        p.put("pass", linesEqual && pass == total);
        return p;
    }

    /** A program with an unreadable output scores 0 of one record's worth of fields, so it can never read as 100%. */
    static Map<String, Object> failed(Map<String, Object> p, List<String> cobol, List<String> java, int expectedFields, String error) {
        p.put("cobolLines", cobol == null ? -1 : cobol.size());
        p.put("javaLines",  java  == null ? -1 : java.size());
        p.put("linesEqual", false);
        p.put("parityPercent", 0);
        p.put("fieldsPassed", "0/" + expectedFields);
        p.put("fieldsPass", 0);
        p.put("fieldsTotal", expectedFields);
        p.put("records", new ArrayList<>());
        p.put("pass", false);
        p.put("error", error);
        return p;
    }

    static List<String> readOutput(String path) throws IOException {
        return Files.readAllLines(Path.of(path)).stream().filter(l -> !l.isBlank()).toList();
    }

    static String field(String line, Field f) {
        if (f.scale > 0 && f.start + f.len + 1 <= line.length() && line.charAt(f.start + f.len) == '.') {
            return line.substring(f.start, f.start + f.len + 1); // tolerate a literal decimal point
        }
        return f.start + f.len <= line.length() ? line.substring(f.start, f.start + f.len) : "<LINE-SHORT>";
    }

    static List<String[]> loadManifest(String path) throws IOException {
        List<String[]> out = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of(path))) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split(",");
            if (p.length != 4) throw new IOException("bad manifest line: " + line);
            out.add(p);
        }
        return out;
    }

    static List<Field> loadFields(String path) throws IOException {
        List<Field> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(",");
                out.add(new Field(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[4])));
            }
        }
        return out;
    }

    static String toJson(Object o) {
        StringBuilder s = new StringBuilder();
        write(s, o);
        return s.toString();
    }
    static void write(StringBuilder s, Object o) {
        if (o instanceof Map<?, ?> m) {
            s.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) s.append(',');
                first = false;
                s.append('"').append(esc(e.getKey().toString())).append("\":");
                write(s, e.getValue());
            }
            s.append('}');
        } else if (o instanceof List<?> l) {
            s.append('[');
            for (int i = 0; i < l.size(); i++) { if (i > 0) s.append(','); write(s, l.get(i)); }
            s.append(']');
        } else if (o instanceof Number || o instanceof Boolean) {
            s.append(o);
        } else {
            s.append('"').append(esc(o.toString())).append('"');
        }
    }
    static String esc(String v) { return v.replace("\\", "\\\\").replace("\"", "\\\""); }
}
