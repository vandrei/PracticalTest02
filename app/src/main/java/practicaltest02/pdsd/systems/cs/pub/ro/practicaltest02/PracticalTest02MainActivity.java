package practicaltest02.pdsd.systems.cs.pub.ro.practicaltest02;

import android.provider.SyncStateContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PracticalTest02MainActivity extends ActionBarActivity {
    ServerSocket serverSocket;
    Button startServerButton;
    Button getButton;
    Button putButton;

    Date lastRefreshTime;

    HashMap<String, String> data = new HashMap<String, String>();

    Thread serverThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practical_test02_main);

        startServerButton = (Button)findViewById(R.id.connect_button);
        startServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initServer();
            }
        });

        getButton = (Button)findViewById(R.id.get_value_button);
        putButton = (Button)findViewById(R.id.put_value_button);

        getButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getValue();
            }
        });

        putButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                putValue();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (serverThread != null) {
            try {
                serverSocket.close();
                serverThread.interrupt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        super.onDestroy();
    }

    private void initServer() {
        int port;
        EditText portView = (EditText)this.findViewById(R.id.server_port_edit_text);
        port = Integer.parseInt(portView.getText().toString());
        try {
            serverSocket = new ServerSocket(port);
            startServerButton.setEnabled(false);
            serverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            final Socket socket = serverSocket.accept();
                            Thread communicationThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                                        if (bufferedReader != null && printWriter != null) {
                                            String line = bufferedReader.readLine();
                                            String[] tokens = line.split(",");
                                            if (tokens != null && tokens.length > 0) {
                                                HttpClient httpClient = new DefaultHttpClient();
                                                HttpGet getRequest = new HttpGet("http://www.timeapi.org/utc/now");
                                                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                                                String timeContent = httpClient.execute(getRequest, responseHandler);
                                                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
                                                if (lastRefreshTime == null) {
                                                    lastRefreshTime = df.parse(timeContent);
                                                } else {
                                                    Date nowDate = df.parse(timeContent);
                                                    long minutes = getDateDiff(lastRefreshTime, nowDate, TimeUnit.MINUTES);

                                                    if (minutes > 0) {
                                                        data = new HashMap<String, String>();
                                                    }
                                                }

                                                if (tokens[0].equals("get")) {
                                                    String key = tokens[1];
                                                    String returnString;
                                                    if (data.containsKey(key)) {
                                                        returnString = data.get(key) + "\n";
                                                    } else {
                                                        returnString = "none\n";
                                                    }

                                                    printWriter.write(returnString);
                                                    printWriter.flush();
                                                } else if (tokens[0].equals("put")) {
                                                    String key = tokens[1];
                                                    String value = tokens[2];

                                                    data.put(key, value);
                                                }
                                            }
                                        }
                                        socket.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            communicationThread.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                }
            });
            serverThread.start();
        } catch (IOException ioException) {
        }
    }

    private void getValue() {
        EditText addressTextView = (EditText)findViewById(R.id.client_address_edit_text);
        EditText portTextView = (EditText)findViewById(R.id.client_port_edit_text);

        final String ipAddress = addressTextView.getText().toString();
        final int port = Integer.parseInt(portTextView.getText().toString());

        EditText keyText = (EditText)findViewById(R.id.key_edit_text);
        final String key = keyText.getText().toString();

        Thread requestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(ipAddress, port);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                    if (bufferedReader != null && printWriter != null) {
                        printWriter.println("get," + key + "\n");
                        printWriter.flush();
                    }
                    final String result = bufferedReader.readLine();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            EditText valueTextView = (EditText)findViewById(R.id.value_text_view);
                            valueTextView.setText(result);
                        }
                    });
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        requestThread.start();
    }

    private void putValue() {
        EditText addressTextView = (EditText)findViewById(R.id.client_address_edit_text);
        EditText portTextView = (EditText)findViewById(R.id.client_port_edit_text);

        final String ipAddress = addressTextView.getText().toString();
        final int port = Integer.parseInt(portTextView.getText().toString());

        EditText keyText = (EditText)findViewById(R.id.key_edit_text);
        final String key = keyText.getText().toString();

        EditText valueText = (EditText)findViewById(R.id.value_text_view);
        final String value = valueText.getText().toString();

        Thread requestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(ipAddress, port);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                    if (bufferedReader != null && printWriter != null) {
                        printWriter.println("put," + key + "," + value + "\n");
                        printWriter.flush();
                    }
                    final String result = bufferedReader.readLine();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            EditText valueTextView = (EditText)findViewById(R.id.value_text_view);
                            valueTextView.setText(result);
                        }
                    });
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        requestThread.start();
    }

    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }
}