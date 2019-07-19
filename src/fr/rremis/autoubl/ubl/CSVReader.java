package fr.rremis.autoubl.ubl;

import java.util.ArrayList;
import java.util.List;

public class CSVReader {
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String[] parseLine(String line) {
		List fields = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == ',') {
				fields.add(sb.toString());
				sb = new StringBuilder();
			} else if (c == '"') {
				int ends = line.indexOf('"', i + 1);
				if (ends == -1) {
					throw new IllegalArgumentException(new StringBuilder()
							.append("Expected double-quote to terminate (")
							.append(i).append("): ").append(line).toString());
				}
				sb.append(line.substring(i + 1, ends - 1));
				i = ends;
			} else {
				sb.append(c);
			}
		}
		fields.add(sb.toString());
		return (String[]) fields.toArray(new String[fields.size()]);
	}
}