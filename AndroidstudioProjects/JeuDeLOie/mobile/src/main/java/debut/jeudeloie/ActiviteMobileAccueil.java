package debut.jeudeloie;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.ParticipantResult;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import debut.jeudeloie.commun.AdapteurJoueurs;
import debut.jeudeloie.commun.Declarations;
import debut.jeudeloie.commun.DonneesJoueur;

import static com.google.android.gms.common.ConnectionResult.SIGN_IN_REQUIRED;

public class ActiviteMobileAccueil extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        Connections.EndpointDiscoveryListener, Connections.ConnectionResponseCallback, Connections.MessageListener,
        OnInvitationReceivedListener, OnTurnBasedMatchUpdateReceivedListener, TextToSpeech.OnInitListener {
    private static final String KEY_IN_RESOLUTION = "is_in_resolution";
    // Discovery timeout, in milliseconds.
    private static final long TIMEOUT_DISCOVER = 10000L;

    private static final int CODE_RETOUR_SIGNIN = 1000;
    private GoogleApiClient mGoogleApiClient;
    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean mIsInResolution;
    private DonneesJoueur mPlayerData;

    private ListView listejoueurs;
    private AdapteurJoueurs adapt;
    private FloatingActionButton btnjouer;

    private String remoteTVid;
    private String matchId;

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }
        setContentView(R.layout.mobile_accueil);
        mPlayerData = new DonneesJoueur();
        tts = new TextToSpeech(this, this);
        listejoueurs = (ListView) findViewById(R.id.listejoueurs);
        adapt = new AdapteurJoueurs(this, R.layout.vuejoueur);
        listejoueurs.setAdapter(adapt);
        btnjouer = (FloatingActionButton) findViewById(R.id.boutonjouer);
        btnjouer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject jo = new JSONObject();
                try {
                    jo.put(Declarations.TAG_JSON_ENTETE, Declarations.COMMANDE_LANCER_DE);
                    jo.put(Declarations.TAG_JSON_JOUEUR, mPlayerData.getIdentifiantJoueur());
                    Log.d(Declarations.TAG_DEBUG, "jouerTour : envoi message : remote : " + remoteTVid);
                    Nearby.Connections.sendReliableMessage(mGoogleApiClient, remoteTVid, jo.toString().getBytes());
                    btnjouer.setVisibility(View.INVISIBLE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Games.API).addApi(Plus.API)
                    .addScope(Games.SCOPE_GAMES)
                    .addScope(Plus.SCOPE_PLUS_PROFILE)
                    .addApi(Nearby.CONNECTIONS_API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    /**
     * Called when activity gets invisible. Connection to Play Services needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : onConnected");
        Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        mPlayerData.setNomComplet(currentPerson.getDisplayName());
        TextView joueurnom = (TextView) findViewById(R.id.joueurtexte);
        joueurnom.setText(mPlayerData.getNomComplet());
        if (currentPerson.hasImage()) {
            mPlayerData.setUrllogo(currentPerson.getImage().getUrl());
            ImageView joueurtete = (ImageView) findViewById(R.id.joueurimage);
            Picasso.with(this).load(mPlayerData.getUrllogo()).into(joueurtete);
        }
        mPlayerData.setIdentifiantJoueur(Games.Players.getCurrentPlayerId(mGoogleApiClient));
        mPlayerData.setPrenom(currentPerson.getName().getGivenName());
        // Discover nearby apps that are advertising with the required service ID.
        Nearby.Connections.startDiscovery(mGoogleApiClient, getPackageName(), TIMEOUT_DISCOVER, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    // Device is discovering.
                    Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections.startDiscovery : onResult : OK");
                } else {
                    int statusCode = status.getStatusCode();
                    // Advertising failed - see statusCode for more details
                    Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections.startDiscovery : onResult : " + status.getStatusMessage());
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : onConnectionSuspended : cause : " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : onConnectionFailed : " + connectionResult.getErrorCode());
        switch (connectionResult.getErrorCode()) {
            case SIGN_IN_REQUIRED:
                Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : onConnectionFailed : SIGN_IN_REQUIRED");
                try {
                    connectionResult.startResolutionForResult(ActiviteMobileAccueil.this, CODE_RETOUR_SIGNIN);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(Declarations.TAG_DEBUG, "onActivityResult : requestCode : " + requestCode);
        switch (requestCode) {
            case CODE_RETOUR_SIGNIN:
                Log.d(Declarations.TAG_DEBUG, "onActivityResult : requestCode : CODE_RETOUR_SIGNIN");
                mGoogleApiClient.connect();
                break;
        }
    }

    @Override
    public void onEndpointFound(String endpointId, String deviceId, String serviceId, String name) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections.startDiscovery : onEndpointFound");
        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);
        // Send a connection request to a remote endpoint. By passing 'null' for the name,
        // the Nearby Connections API will construct a default name based on device model
        // such as 'LGE Nexus 5'.
        TextView tvtv = (TextView) findViewById(R.id.tvtexte);
        tvtv.setText(name);
        String myName = null;
        byte[] myPayload = mPlayerData.getjson().toString().getBytes();
        Nearby.Connections.sendConnectionRequest(mGoogleApiClient, myName, endpointId, myPayload, this, this);
    }

    @Override
    public void onEndpointLost(String endpointId) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections.startDiscovery : onEndpointLost");
    }

    @Override
    public void onConnectionResponse(String remoteEndpointId, Status status, byte[] payload) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections.sendConnectionRequest : onConnectionResponse");
        String donnees = new String(payload);
        ArrayList<DonneesJoueur> lj = DonneesJoueur.fromjsonliste(donnees);
        adapt.clear();
        adapt.addAll(lj);
        adapt.notifyDataSetChanged();
        if (status.isSuccess()) {
            // Successful connection
            remoteTVid = remoteEndpointId;
//            Nearby.Connections.stopDiscovery(mGoogleApiClient, getPackageName());
        } else {
            // Failed connection
        }
    }

    @Override
    public void onMessageReceived(String remoteEndpointId, byte[] payload, boolean isReliable) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections : onMessageReceived");
        try {
            JSONObject jo = new JSONObject(new String(payload));
            String entete = jo.getString(Declarations.TAG_JSON_ENTETE);
            if (entete.equals(Declarations.REPONSE_LANCER_DE)) {
                String don = jo.getString(Declarations.TAG_JSON_DONNEES);
                ArrayList<DonneesJoueur> lj = DonneesJoueur.fromjsonliste(don);
                adapt.clear();
                adapt.addAll(lj);
                adapt.notifyDataSetChanged();
                int maPosition = 0;
                for (DonneesJoueur jd : lj) {
                    if (jd.getIdentifiantJoueur().equals(mPlayerData.getIdentifiantJoueur())) {
                        maPosition = jd.getPosition();
                    }
                }
                if (maPosition != 63) {
                    if (jo.has(Declarations.TAG_JSON_JOUEUR)) {
                        String idjsuivant = jo.getString(Declarations.TAG_JSON_JOUEUR);
                        Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, matchId, don.getBytes(), idjsuivant)
                                .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                                    @Override
                                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
                                        Log.d(Declarations.TAG_DEBUG, "onMessageReceived : Games.TurnBasedMultiplayer.takeTurn : updateMatchResult succès : "
                                                + updateMatchResult.getStatus().isSuccess());
                                    }
                                });
                    }
                } else {
                    List<ParticipantResult> resultats = new ArrayList<>();
                    for (DonneesJoueur dj : lj) {
                        ParticipantResult res;
                        int idxj = lj.indexOf(dj);
                        if (dj.getIdentifiantJoueur().equals(mPlayerData.getIdentifiantJoueur())) {
                            res = new ParticipantResult(dj.getIdentifiantParticipant(),
                                    ParticipantResult.MATCH_RESULT_WIN, ParticipantResult.PLACING_UNINITIALIZED);
                            dj.setGagnant(true);
                        } else {
                            res = new ParticipantResult(dj.getIdentifiantParticipant(),
                                    ParticipantResult.MATCH_RESULT_LOSS, ParticipantResult.PLACING_UNINITIALIZED);
                            dj.setGagnant(false);
                        }
                        resultats.add(res);
                        lj.set(idxj, dj);
                    }
                    adapt.clear();
                    adapt.addAll(lj);
                    adapt.notifyDataSetChanged();
                    Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, matchId, DonneesJoueur.tojsonliste(lj).getBytes(), resultats)
                            .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                                @Override
                                public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
                                    Log.d(Declarations.TAG_DEBUG, "Games.TurnBasedMultiplayer.finishMatch : updateMatchResult succès : "
                                            + updateMatchResult.getStatus().isSuccess());
                                }
                            });
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnected(String remoteEndpointId) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections.sendConnectionRequest : onDisconnected");
    }

    @Override
    public void onInvitationReceived(Invitation invitation) {
        Log.d(Declarations.TAG_DEBUG, "GoogleApiClient : onInvitationReceived");
        Games.TurnBasedMultiplayer.acceptInvitation(mGoogleApiClient, invitation.getInvitationId()).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.InitiateMatchResult initiateMatchResult) {
                        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Games.TurnBasedMultiplayer.acceptInvitation");
                        if (initiateMatchResult.getStatus().isSuccess()) {
                            Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Games.TurnBasedMultiplayer.acceptInvitation : OK");
                            Games.TurnBasedMultiplayer.registerMatchUpdateListener(mGoogleApiClient, ActiviteMobileAccueil.this);
                            matchId = initiateMatchResult.getMatch().getMatchId();
                            jouerTour(initiateMatchResult.getMatch());
                        } else {
                            Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Games.TurnBasedMultiplayer.acceptInvitation : "
                                    + initiateMatchResult.getStatus().getStatusMessage());
                        }
                    }
                });
    }

    private void jouerTour(TurnBasedMatch match) {
        String donnees = new String(match.getData());
        Log.d(Declarations.TAG_DEBUG, "jouerTour : donnees : " + donnees);
        final ArrayList<DonneesJoueur> lj = DonneesJoueur.fromjsonliste(donnees);
        adapt.clear();
        adapt.addAll(lj);
        adapt.notifyDataSetChanged();
        if (match.getStatus() == TurnBasedMatch.MATCH_STATUS_ACTIVE) {
            Log.d(Declarations.TAG_DEBUG, "jouerTour : match actif");
            if (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                Log.d(Declarations.TAG_DEBUG, "jouerTour : MY TURN");
                speakOut(this.getString(R.string.amontour));
                btnjouer.setVisibility(View.VISIBLE);
            } else if (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN) {
                Log.d(Declarations.TAG_DEBUG, "jouerTour : THEIR TURN");
            }
        } else if (match.getStatus() == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
            Log.d(Declarations.TAG_DEBUG, "jouerTour : match terminé");
            Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, matchId).setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                @Override
                public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
                    Log.d(Declarations.TAG_DEBUG, "finMatch : finishMatch : onResult : succès : " + updateMatchResult.getStatus().isSuccess());
                }
            });
/*            Games.TurnBasedMultiplayer.loadMatch(mGoogleApiClient, matchId)
                    .setResultCallback(new ResultCallback<TurnBasedMultiplayer.LoadMatchResult>() {
                        @Override
                        public void onResult(TurnBasedMultiplayer.LoadMatchResult loadMatchResult) {
                            Log.d(Declarations.TAG_DEBUG, "finMatch : loadMatch : onResult");
                            for (Participant par : loadMatchResult.getMatch().getParticipants()) {
                                Log.d(Declarations.TAG_DEBUG, "finMatch : loadMatch : participant : " + par.getParticipantId());
                                for (DonneesJoueur dj : lj) {
                                    Log.d(Declarations.TAG_DEBUG, "finMatch : loadMatch : mémoire : " + dj.getIdentifiantParticipant());
                                    int idxj = lj.indexOf(dj);
                                    if (par.getPlayer().getPlayerId().equals(dj.getIdentifiantJoueur())) {
                                        if (par.getResult().equals(ParticipantResult.MATCH_RESULT_WIN)) {
                                            Log.d(Declarations.TAG_DEBUG, "jouerTour : MATCH_RESULT_WIN : " + par.getParticipantId());
                                            dj.setGagnant(true);
                                            lj.set(idxj, dj);
                                        } else if (par.getResult().equals(ParticipantResult.MATCH_RESULT_DISAGREED)) {
                                            Log.d(Declarations.TAG_DEBUG, "jouerTour : MATCH_RESULT_DISAGREED : " + par.getParticipantId());
                                            dj.setGagnant(false);
                                            lj.set(idxj, dj);
                                        } else if (par.getResult().equals(ParticipantResult.MATCH_RESULT_DISCONNECT)) {
                                            Log.d(Declarations.TAG_DEBUG, "jouerTour : MATCH_RESULT_DISCONNECT : " + par.getParticipantId());
                                            dj.setGagnant(false);
                                            lj.set(idxj, dj);
                                        } else if (par.getResult().equals(ParticipantResult.MATCH_RESULT_NONE)) {
                                            Log.d(Declarations.TAG_DEBUG, "jouerTour : MATCH_RESULT_NONE : " + par.getParticipantId());
                                            dj.setGagnant(false);
                                            lj.set(idxj, dj);
                                        } else {
                                            Log.d(Declarations.TAG_DEBUG, "jouerTour : perdant : " + par.getParticipantId());
                                            dj.setGagnant(false);
                                            lj.set(idxj, dj);
                                        }
                                    }
                                }
                            }
                            adapt.clear();
                            adapt.addAll(lj);
                            adapt.notifyDataSetChanged();
                        }
                    });*/
        }
    }

    @Override
    public void onInvitationRemoved(String invitationId) {
        Log.d(Declarations.TAG_DEBUG, "GoogleApiClient : onInvitationRemoved");
    }

    @Override
    public void onTurnBasedMatchReceived(TurnBasedMatch turnBasedMatch) {
        String donnees = new String(turnBasedMatch.getData());
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : onTurnBasedMatchReceived : donnees : " + donnees);
        jouerTour(turnBasedMatch);
    }

    @Override
    public void onTurnBasedMatchRemoved(String matchId) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : onTurnBasedMatchRemoved : matchId : " + matchId);
    }

    private void speakOut(String texte) {
        tts.speak(texte, TextToSpeech.QUEUE_ADD, null, String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(Declarations.TAG_DEBUG, "Speech synthesis : this Language is not supported");
            } else {
                speakOut(this.getString(R.string.bienvenue) + " " + this.getString(R.string.app_name));
            }

        } else {
            Log.e(Declarations.TAG_DEBUG, "Speech synthesis : initialization failed!");
        }
    }

}
