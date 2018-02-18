package org.upperlevel.corrida.phase.game;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.upperlevel.corrida.R;
import org.upperlevel.corrida.command.Command;
import org.upperlevel.corrida.command.PlayerJoinCommand;
import org.upperlevel.corrida.command.PlayerQuitCommand;
import org.upperlevel.corrida.command.PlayersCommand;
import org.upperlevel.corrida.command.StartRequestCommand;
import org.upperlevel.corrida.phase.Phase;
import org.upperlevel.corrida.phase.game.playing.PlayingPhase;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;

public class LobbyPhase implements Phase {
    public static final String TAG = LobbyPhase.class.getSimpleName();

    @Getter
    private Activity activity;

    @Getter
    private Game game;

    @Getter
    private Thread waitStartTask; // issued when the start button is pressed

    @Getter
    private TextView playersDisplay;

    public LobbyPhase(Game game) {
        this.activity = game.getActivity();
        this.game = game;
    }

    @Override
    public void onStart() {
        activity.setContentView(R.layout.activity_wait_start);
        playersDisplay = activity.findViewById(R.id.players_count_value);
        activity.findViewById(R.id.start_game_button).setOnClickListener(new OnGameStartClick());

        waitStartTask = new WaitStartTask();
        waitStartTask.start();
    }

    @Override
    public void onStop() {
        if (waitStartTask.isAlive()) {
            waitStartTask.interrupt();
        }
    }

    /**
     * Updates players count showed if needed and
     * listens for "game_start" packet.
     */
    public class WaitStartTask extends Thread {
        @Override
        public void run() {
            boolean repeat = true;
            while (repeat) {
                String[] received;
                try {
                    received = Command.split(game.receive());
                } catch (IOException e) {
                    Toast.makeText(activity, "Errore di ricezione", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    return;
                }
                switch (received[0]) {
                    case "players":
                        PlayersCommand playersCmd = new PlayersCommand().decode(received);
                        activity.runOnUiThread(() -> {
                            for (String name : playersCmd.getPlayers()) {
                                game.addPlayer(new Player(name));
                            }
                            int size = game.getPlayers().size();
                            playersDisplay.setText(size + "");
                            Log.i(TAG, "Received 'players' packet. Updated player number field.");
                        });
                        break;
                    case "player_join":
                        PlayerJoinCommand playerJoinCmd = new PlayerJoinCommand().decode(received);
                        activity.runOnUiThread(() -> {
                            game.addPlayer(new Player(playerJoinCmd.getPlayer()));
                            int size = game.getPlayers().size();
                            playersDisplay.setText(size + "");
                            Log.i(TAG, "Received 'player_join' packet. Updated player number field.");
                        });
                        break;
                    case "player_quit":
                        PlayerQuitCommand playerQuitCmd = new PlayerQuitCommand().decode(received);
                        activity.runOnUiThread(() -> {
                            game.removePlayer(playerQuitCmd.getPlayer());
                            int size = game.getPlayers().size();
                            playersDisplay.setText(size + "");
                            Log.i(TAG, "Received 'player_quit' packet. Updated player number field.");
                        });
                        break;
                    case "game_start":
                        activity.runOnUiThread(() -> {
                            game.setPhase(new PlayingPhase(game));
                            Log.i(TAG, "Received a 'game_start' packet. Ready to play!");
                        });
                        repeat = false;
                        break;
                    default:
                        Log.e(TAG, "Received an unhandled packet: " + received[0]);
                        break;
                }
            }
            Log.i(TAG, "Listening task stopped! Has 'game_start' packet been received?");
        }
    }

    /**
     * Listens for button that sends the "game_start" packet.
     */
    public class OnGameStartClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            try {
                if (game.canStart()) {
                    game.emit(new StartRequestCommand());
                    Log.i(TAG, "There are enough players to start the game!");
                } else {
                    Toast.makeText(activity, "Sto aspettando ulteriori giocatori!", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Too few players!");
                }
            } catch (IOException e) {
                Toast.makeText(activity, "Impossibile inviare richiesta", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }
}
