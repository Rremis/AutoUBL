package fr.rremis.autoubl.ubl;

import com.google.common.collect.ImmutableList;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class UUIDFetcher
  implements Callable<Map<String, UUID>>
{

  private final JSONParser jsonParser = new JSONParser();
  private final List<String> names;
  private final boolean rateLimiting;

  public UUIDFetcher(List<String> names, boolean rateLimiting)
  {
    this.names = ImmutableList.copyOf(names);
    this.rateLimiting = rateLimiting;
  }

  public UUIDFetcher(List<String> names) {
    this(names, true);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
public Map<String, UUID> call() throws Exception
  {
    Map uuidMap = new HashMap<>();
    int requests = (int)Math.ceil(this.names.size() / 100.0D);
    for (int i = 0; i < requests; i++) {
      HttpURLConnection connection = createConnection();
      String body = JSONArray.toJSONString(this.names.subList(i * 100, Math.min((i + 1) * 100, this.names.size())));
      writeBody(connection, body);
      JSONArray array = (JSONArray)this.jsonParser.parse(new InputStreamReader(connection.getInputStream()));
      for (Iterator i$ = array.iterator(); i$.hasNext(); ) { Object profile = i$.next();
        JSONObject jsonProfile = (JSONObject)profile;
        String id = (String)jsonProfile.get("id");
        String name = (String)jsonProfile.get("name");
        UUID uuid = getUUID(id);
        uuidMap.put(name, uuid);
      }
      if ((this.rateLimiting) && (i != requests - 1)) {
        Thread.sleep(100L);
      }
    }
    return uuidMap;
  }

  private static void writeBody(HttpURLConnection connection, String body) throws Exception {
    OutputStream stream = connection.getOutputStream(); Throwable localThrowable2 = null;
    try { stream.write(body.getBytes());
      stream.flush();
    }
    catch (Throwable localThrowable1)
    {
      localThrowable2 = localThrowable1; throw localThrowable1;
    }
    finally {
      if (stream != null) if (localThrowable2 != null) try { stream.close(); } catch (Throwable x2) { localThrowable2.addSuppressed(x2); } else stream.close();  
    }
  }

  private static HttpURLConnection createConnection() throws Exception {
    URL url = new URL("https://api.mojang.com/profiles/minecraft");
    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setUseCaches(false);
    connection.setDoInput(true);
    connection.setDoOutput(true);
    return connection;
  }

  private static UUID getUUID(String id) {
    return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
  }

  public static byte[] toBytes(UUID uuid) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
    byteBuffer.putLong(uuid.getMostSignificantBits());
    byteBuffer.putLong(uuid.getLeastSignificantBits());
    return byteBuffer.array();
  }

  public static UUID fromBytes(byte[] array) {
    if (array.length != 16) {
      throw new IllegalArgumentException("Illegal byte array length: " + array.length);
    }
    ByteBuffer byteBuffer = ByteBuffer.wrap(array);
    long mostSignificant = byteBuffer.getLong();
    long leastSignificant = byteBuffer.getLong();
    return new UUID(mostSignificant, leastSignificant);
  }

  public static UUID getUUIDOf(String name) throws Exception {
    return (UUID)new UUIDFetcher(Arrays.asList(new String[] { name })).call().get(name);
  }
}
