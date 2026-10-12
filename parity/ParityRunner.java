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
 * Parity runner: byte-for-byte field-level diff between the COBOL reference output
 * (OUT-COBOL.DAT) and the Java modernized output (OUT-JAVA.DAT), using the copybook
 * layout in copybook.txt. Writes result.json with per-record / per-field results.
 */
public final class ParityRunner {

    static final class Field {
        String name; int start, len, scale;
        Field(String n, int s, int l, int sc) { name = n; start = s; len = l; scale = sc; }
    }

    public static void main(String[] args) throws Exception {
        String cobolPath = args.length > 0 ? args[0] : "../cobol/OUT-COBOL.DAT";
        String javaPath = args.length > 1 ? args[1] : "../java/OUT-JAVA.DAT";
        String outPath = args.length > 2 ? args[2] : "result.json";

        List<Field> fields = loadFields("copybook.txt");
        List<String> cobol = Files.readAllLines(Path.of(cobolPath));
        List<String> java = Files.readAllLines(Path.of(javaPath));

        int allFields = 0, allPass = 0;
        int matchRows = Math.min(cobol.size(), java.size());
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < matchRows; i++) {
            String c = cobol.get(i), j = java.get(i);
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("record", i + 1);
            int rowPass = 0;
            List<Map<String, Object>> flds = new ArrayList<>();
            for (Field f : fields) {
                String cv = field(c, f), jv = field(j, f);
                boolean pass = cv.equals(jv);
                if (pass) rowPass++;
                Map<String, Object> ff = new LinkedHashMap<>();
                ff.put("name", f.name); ff.put("pass", pass);
                if (!pass) { ff.put("cobol", cv); ff.put("java", jv); }
                flds.add(ff);
            }
            rec.put("fields", flds);
            rec.put("rowPass", rowPass + "/" + fields.size());
            records.add(rec);
            allFields += fields.size();
            allPass += rowPass;
        }
        boolean filesEqual = cobol.size() == java.size();

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("cobolLines", cobol.size());
        root.put("javaLines", java.size());
        root.put("linesEqual", filesEqual);
        root.put("parityPercent", allFields == 0 ? 0 : Math.round(allPass * 10000.0 / allFields) / 100.0);
        root.put("fieldsPassed", allPass + "/" + allFields);
        root.put("records", records);
        root.put("pass", filesEqual && allPass == allFields);

        try (FileWriter w = new FileWriter(outPath)) {
            w.write(toJson(root));
        }
        System.out.println("parity = " + (allPass) + "/" + allFields + " fields | "
                + (filesEqual ? "" : "LINE-COUNT MISMATCH ") + (root.get("pass").equals(Boolean.TRUE) ? "PASS" : "FAIL"));
    }

    static String field(String line, Field f) {
        if (f.scale > 0 && f.start + f.len + 1 <= line.length() && line.charAt(f.start + f.len) == '.') {
            return line.substring(f.start, f.start + f.len + 1); // tolerate a literal decimal point
        }
        return f.start + f.len <= line.length() ? line.substring(f.start, f.start + f.len) : "<LINE-SHORT>";
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
    @SuppressWarnings("unchecked")
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