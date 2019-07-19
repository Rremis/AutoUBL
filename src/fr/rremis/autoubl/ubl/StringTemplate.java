package fr.rremis.autoubl.ubl;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class StringTemplate{
  private String[] text;
  private String[] fieldNames;

  private StringTemplate(String[] text, String[] fieldNames){
    this.text = text;
    this.fieldNames = fieldNames;
  }

  public String format(Map<String, String> data){
    StringBuilder sb = new StringBuilder(this.text[0]);
    for (int i = 0; i < this.fieldNames.length; i++) {
      sb.append((String)data.get(this.fieldNames[i]));
      sb.append(this.text[(i + 1)]);
    }
    return sb.toString();
  }

  public List<String> getText(){
    return Arrays.asList(this.text);
  }

  public List<String> getFieldNames(){
    return Arrays.asList(this.fieldNames);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
public static StringTemplate getStringTemplate(String template){
    List textList = new ArrayList<>();
    List fieldNameList = new ArrayList<>();
    StringBuilder textBuilder = new StringBuilder();
    StringBuilder fieldNameBuilder = null;
    for (int i = 0; i < template.length(); i++) {
      char c = template.charAt(i);
      if (textBuilder != null) {
        if (c == '{') {
          textList.add(textBuilder.toString());
          textBuilder = null;
          fieldNameBuilder = new StringBuilder(); } else {
          if (c == '}') {
            throw new IllegalArgumentException(new StringBuilder().append("Invalid template. Missing open curly brace '{' in: ").append(template).toString());
          }
          textBuilder.append(c);
        }
      } else if (c == '}') {
        fieldNameList.add(fieldNameBuilder.toString());
        fieldNameBuilder = null;
        textBuilder = new StringBuilder(); } else {
        if (c == '}') {
          throw new IllegalArgumentException(new StringBuilder().append("Invalid template. Missing closing curly brace '}' in: ").append(template).toString());
        }
        fieldNameBuilder.append(c);
      }
    }

    if (fieldNameBuilder != null) {
      throw new IllegalArgumentException(new StringBuilder().append("Invalid template. Missing closing curly brace '}' in: ").append(template).toString());
    }
    textList.add(textBuilder.toString());
    return new StringTemplate((String[])textList.toArray(new String[textList.size()]), (String[])fieldNameList.toArray(new String[fieldNameList.size()]));
  }
}
