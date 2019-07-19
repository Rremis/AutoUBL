package fr.rremis.autoubl.ubl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BanEntry {

	private Map<String, String> data = new HashMap<>();
	private String ign;
	private UUID uuid;

	public BanEntry(String[] fieldNames, String rawCSV) {

		String[] parts = CSVReader.parseLine(rawCSV);
		if (parts.length != fieldNames.length) {
			throw new IllegalArgumentException("Expected " + fieldNames.length
					+ " columns: " + rawCSV);
		}
		for (int i = 0; i < fieldNames.length; i++) {
			this.data.put(fieldNames[i], parts[i]);
		}
	}

	public void setIgn(String ign) {
		this.ign = ign;
	}

	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}

	public String getIgn() {
		return this.ign;
	}

	public UUID getUUID() {
		return this.uuid;
	}

	public String getData(String fieldName) {
		return (String) this.data.get(fieldName);
	}

	public void setData(String fieldName, String value) {
		this.data.put(fieldName, value);
	}

	public Map<String, String> getData() {
		return this.data;
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if ((obj instanceof BanEntry)) {
			BanEntry other = (BanEntry) obj;
			if ((other.uuid != null) && (this.uuid != null)) {
				return other.uuid.equals(this.uuid);
			}
			return other.ign.equalsIgnoreCase(this.ign);
		}
		return false;
	}

	public int hashCode() {
		if (this.uuid != null) {
			return this.uuid.hashCode();
		}
		return this.ign.hashCode();
	}
}