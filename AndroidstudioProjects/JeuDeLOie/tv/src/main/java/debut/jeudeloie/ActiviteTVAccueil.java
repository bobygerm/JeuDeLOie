package debut.jeudeloie;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.ParticipantResult;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AppIdentifier;
import com.google.android.gms.nearby.connection.AppMetadata;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import debut.jeudeloie.commun.Declarations;
import debut.jeudeloie.commun.DonneesJoueur;

import static com.google.android.gms.common.ConnectionResult.SIGN_IN_REQUIRED;

public class ActiviteTVAccueil extends Activity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Connections.ConnectionRequestListener,
        Connections.MessageListener, TextToSpeech.OnInitListener, OnTurnBasedMatchUpdateReceivedListener {
    private static final int CODE_RETOUR_SIGNIN = 1000;
    private static final String KEY_IN_RESOLUTION = "is_in_resolution";

    private GoogleApiClient mGoogleApiClient;
    private boolean mIsInResolution;
    private ArrayList<DonneesJoueur> mPlayerData;
    private int nbjoueurs;
    private ArrayList<String> invites;
    private TextView tvattente;
    private ImageButton btnplay;
    private Random hasard;
    private TextView tvde;
    private ImageButton btntour;
    private String mMatchId;

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }
        setContentView(R.layout.tv_accueil);
        mPlayerData = new ArrayList<>();
        nbjoueurs = 1;
        invites = new ArrayList<>();
        tvattente = (TextView) findViewById(R.id.texteAccueil);
        btnplay = (ImageButton) findViewById(R.id.imageStart);
        btnplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(Declarations.TAG_DEBUG, "Lancement partie");
                Nearby.Connections.stopAdvertising(mGoogleApiClient);
                if (!invites.isEmpty()) {
                    TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                            .addInvitedPlayers(invites)
                            .setAutoMatchCriteria(null)
                            .build();
                    // Create and start the match.
                    Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(new MatchInitiatedCallback());
                } else {
                    hasard = new Random();
                    showTurnUI(mPlayerData.get(0).getIdentifiantJoueur());
                }
                tvattente.setVisibility(View.INVISIBLE);
                btnplay.setVisibility(View.INVISIBLE);
            }
        });
        tvde = (TextView) findViewById(R.id.tvcoupde);
        tts = new TextToSpeech(this, this);
        btntour = (ImageButton) findViewById(R.id.imageJouer);
        btntour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(Declarations.TAG_DEBUG, "Je prends mon tour");
                btntour.setVisibility(View.INVISIBLE);
                showTurnUI(mPlayerData.get(0).getIdentifiantJoueur());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(Declarations.TAG_DEBUG, "ActiviteTVAccueil : onStart");
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
    public void onConnected(Bundle bundle) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : onConnected");
        Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        DonneesJoueur dj = new DonneesJoueur();
        dj.setNomComplet(currentPerson.getDisplayName());
        TextView joueurnom = (TextView) findViewById(R.id.joueur1_nom);
        joueurnom.setText(dj.getNomComplet());
        if (currentPerson.hasImage()) {
            dj.setUrllogo(currentPerson.getImage().getUrl());
            ImageView joueurtete = (ImageView) findViewById(R.id.joueur1_image);
            Picasso.with(this).load(dj.getUrllogo()).into(joueurtete);
        }
        dj.setIdentifiantJoueur(Games.Players.getCurrentPlayerId(mGoogleApiClient));
        dj.setPrenom(currentPerson.getName().getGivenName());
        dj.setNumero(1);
//        dj.setPosition(1);
        mPlayerData.add(dj);
        deplacerPion(0, 1, false, false);
        speakOut(mPlayerData.get(0).getPrenom());
        // Advertising with an AppIdentifer lets other devices on the
        // network discover this application and prompt the user to
        // install the application.
        List<AppIdentifier> appIdentifierList = new ArrayList<>();
        appIdentifierList.add(new AppIdentifier(getPackageName()));
        AppMetadata appMetadata = new AppMetadata(appIdentifierList);
        // The advertising timeout is set to run indefinitely
        // Positive values represent timeout in milliseconds
        long NO_TIMEOUT = 0L;
        String name = null;
        Nearby.Connections.startAdvertising(mGoogleApiClient, name, appMetadata, NO_TIMEOUT, this)
                .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                    @Override
                    public void onResult(Connections.StartAdvertisingResult result) {
                        if (result.getStatus().isSuccess()) {
                            // Device is advertising
                            Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections.startAdvertising : onResult : OK");
                        } else {
                            int statusCode = result.getStatus().getStatusCode();
                            // Advertising failed - see statusCode for more details
                            Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections.startAdvertising : onResult : "
                                    + result.getStatus().getStatusMessage());
                        }
                    }
                });
        btnplay.setVisibility(View.VISIBLE);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : onConnectionFailed : " + connectionResult.getErrorCode());
        switch (connectionResult.getErrorCode()) {
            case SIGN_IN_REQUIRED:
                Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : onConnectionFailed : SIGN_IN_REQUIRED");
                try {
                    connectionResult.startResolutionForResult(ActiviteTVAccueil.this, CODE_RETOUR_SIGNIN);
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
    public void onConnectionRequest(String remoteEndpointId, String remoteDeviceId, String remoteEndpointName, byte[] payload) {
        String donnees = new String(payload);
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections.startAdvertising : onConnectionRequest : " + donnees);
        nbjoueurs = nbjoueurs + 1;
        final DonneesJoueur dj = DonneesJoueur.fromjson(donnees);
//        dj.setPosition(1);
        dj.setNumero(nbjoueurs);
        dj.setRemoteid(remoteEndpointId);
        mPlayerData.add(dj);
        // Automatically accept all requests
        byte[] myPayload = DonneesJoueur.tojsonliste(mPlayerData).getBytes();
        Nearby.Connections.acceptConnectionRequest(mGoogleApiClient, remoteEndpointId, myPayload, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections.acceptConnectionRequest : onResult : OK");
                            getPionJoueurTexte(nbjoueurs).setText(dj.getNomComplet());
                            Picasso.with(ActiviteTVAccueil.this).load(dj.getUrllogo()).into(getPionJoueurImage(nbjoueurs));
                            invites.add(dj.getIdentifiantJoueur());
                            deplacerPion(nbjoueurs - 1, 1, false, false);
                            speakOut(mPlayerData.get(nbjoueurs - 1).getPrenom());
//                            btnplay.setVisibility(View.VISIBLE);
                        } else {
                            Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections.acceptConnectionRequest : onResult : "
                                    + status.getStatusMessage());
                        }
                    }
                });
    }

    @Override
    public void onMessageReceived(String remoteEndpointId, byte[] bytes, boolean isReliable) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections : onMessageReceived : remote : " + remoteEndpointId);
        try {
            JSONObject joquestion = new JSONObject(new String(bytes));
            String commande = joquestion.getString(Declarations.TAG_JSON_ENTETE);
            String idjoueur = joquestion.getString(Declarations.TAG_JSON_JOUEUR);
            Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections.startAdvertising : onMessageReceived : commande : " + commande);
            if (commande.equals(Declarations.COMMANDE_LANCER_DE)) {
                showTurnUI(idjoueur);
/*                JSONObject joreponse = new JSONObject();
                joreponse.put(Declarations.TAG_JSON_ENTETE, Declarations.REPONSE_LANCER_DE);
                joreponse.put(Declarations.TAG_JSON_DONNEES, DonneesJoueur.tojsonliste(mPlayerData).toString());
                joreponse.put(Declarations.TAG_JSON_JOUEUR, trouverParticipantSuivant(idjoueur));
                Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections : onMessageReceived : envoi réponse : " + joreponse.toString());
                Nearby.Connections.sendReliableMessage(mGoogleApiClient, remoteEndpointId, joreponse.toString().getBytes());*/
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnected(String s) {
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : Nearby.Connections.startAdvertising : onDisconnected");
    }

    private View getPionJoueur(int numero) {
        View retour = null;
        switch (numero) {
            case 1:
                retour = findViewById(R.id.joueur1_tout);
                break;
            case 2:
                retour = findViewById(R.id.joueur2_tout);
                break;
            case 3:
                retour = findViewById(R.id.joueur3_tout);
                break;
            case 4:
                retour = findViewById(R.id.joueur4_tout);
                break;
        }
        return retour;
    }

    private TextView getPionJoueurTexte(int numero) {
        TextView retour = null;
        switch (numero) {
            case 1:
                retour = (TextView) findViewById(R.id.joueur1_nom);
                break;
            case 2:
                retour = (TextView) findViewById(R.id.joueur2_nom);
                break;
            case 3:
                retour = (TextView) findViewById(R.id.joueur3_nom);
                break;
            case 4:
                retour = (TextView) findViewById(R.id.joueur4_nom);
                break;
        }
        return retour;
    }

    private ImageView getPionJoueurImage(int numero) {
        ImageView retour = null;
        switch (numero) {
            case 1:
                retour = (ImageView) findViewById(R.id.joueur1_image);
                break;
            case 2:
                retour = (ImageView) findViewById(R.id.joueur2_image);
                break;
            case 3:
                retour = (ImageView) findViewById(R.id.joueur3_image);
                break;
            case 4:
                retour = (ImageView) findViewById(R.id.joueur4_image);
                break;
        }
        return retour;
    }

    private View getVueCase(int numero) {
        View retour = null;
        switch (numero) {
            case 1:
                retour = findViewById(R.id.rlcase1);
                break;
            case 2:
                retour = findViewById(R.id.rlcase2);
                break;
            case 3:
                retour = findViewById(R.id.rlcase3);
                break;
            case 4:
                retour = findViewById(R.id.rlcase4);
                break;
            case 5:
                retour = findViewById(R.id.rlcase5);
                break;
            case 6:
                retour = findViewById(R.id.rlcase6);
                break;
            case 7:
                retour = findViewById(R.id.rlcase7);
                break;
            case 8:
                retour = findViewById(R.id.rlcase8);
                break;
            case 9:
                retour = findViewById(R.id.rlcase9);
                break;
            case 10:
                retour = findViewById(R.id.rlcase10);
                break;
            case 11:
                retour = findViewById(R.id.rlcase11);
                break;
            case 12:
                retour = findViewById(R.id.rlcase12);
                break;
            case 13:
                retour = findViewById(R.id.rlcase13);
                break;
            case 14:
                retour = findViewById(R.id.rlcase14);
                break;
            case 15:
                retour = findViewById(R.id.rlcase15);
                break;
            case 16:
                retour = findViewById(R.id.rlcase16);
                break;
            case 17:
                retour = findViewById(R.id.rlcase17);
                break;
            case 18:
                retour = findViewById(R.id.rlcase18);
                break;
            case 19:
                retour = findViewById(R.id.rlcase19);
                break;
            case 20:
                retour = findViewById(R.id.rlcase20);
                break;
            case 21:
                retour = findViewById(R.id.rlcase21);
                break;
            case 22:
                retour = findViewById(R.id.rlcase22);
                break;
            case 23:
                retour = findViewById(R.id.rlcase23);
                break;
            case 24:
                retour = findViewById(R.id.rlcase24);
                break;
            case 25:
                retour = findViewById(R.id.rlcase25);
                break;
            case 26:
                retour = findViewById(R.id.rlcase26);
                break;
            case 27:
                retour = findViewById(R.id.rlcase27);
                break;
            case 28:
                retour = findViewById(R.id.rlcase28);
                break;
            case 29:
                retour = findViewById(R.id.rlcase29);
                break;
            case 30:
                retour = findViewById(R.id.rlcase30);
                break;
            case 31:
                retour = findViewById(R.id.rlcase31);
                break;
            case 32:
                retour = findViewById(R.id.rlcase32);
                break;
            case 33:
                retour = findViewById(R.id.rlcase33);
                break;
            case 34:
                retour = findViewById(R.id.rlcase34);
                break;
            case 35:
                retour = findViewById(R.id.rlcase35);
                break;
            case 36:
                retour = findViewById(R.id.rlcase36);
                break;
            case 37:
                retour = findViewById(R.id.rlcase37);
                break;
            case 38:
                retour = findViewById(R.id.rlcase38);
                break;
            case 39:
                retour = findViewById(R.id.rlcase39);
                break;
            case 40:
                retour = findViewById(R.id.rlcase40);
                break;
            case 41:
                retour = findViewById(R.id.rlcase41);
                break;
            case 42:
                retour = findViewById(R.id.rlcase42);
                break;
            case 43:
                retour = findViewById(R.id.rlcase43);
                break;
            case 44:
                retour = findViewById(R.id.rlcase44);
                break;
            case 45:
                retour = findViewById(R.id.rlcase45);
                break;
            case 46:
                retour = findViewById(R.id.rlcase46);
                break;
            case 47:
                retour = findViewById(R.id.rlcase47);
                break;
            case 48:
                retour = findViewById(R.id.rlcase48);
                break;
            case 49:
                retour = findViewById(R.id.rlcase49);
                break;
            case 50:
                retour = findViewById(R.id.rlcase50);
                break;
            case 51:
                retour = findViewById(R.id.rlcase51);
                break;
            case 52:
                retour = findViewById(R.id.rlcase52);
                break;
            case 53:
                retour = findViewById(R.id.rlcase53);
                break;
            case 54:
                retour = findViewById(R.id.rlcase54);
                break;
            case 55:
                retour = findViewById(R.id.rlcase55);
                break;
            case 56:
                retour = findViewById(R.id.rlcase56);
                break;
            case 57:
                retour = findViewById(R.id.rlcase57);
                break;
            case 58:
                retour = findViewById(R.id.rlcase58);
                break;
            case 59:
                retour = findViewById(R.id.rlcase59);
                break;
            case 60:
                retour = findViewById(R.id.rlcase60);
                break;
            case 61:
                retour = findViewById(R.id.rlcase61);
                break;
            case 62:
                retour = findViewById(R.id.rlcase62);
                break;
            case 63:
                retour = findViewById(R.id.rlcase63);
                break;
        }
        return retour;
    }

    /* Synthèse vocale */
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

    private void speakOut(String texte) {
        tts.speak(texte, TextToSpeech.QUEUE_ADD, null, String.valueOf(System.currentTimeMillis()));
    }

    /* initialisation match */
    public class MatchInitiatedCallback implements ResultCallback<TurnBasedMultiplayer.InitiateMatchResult> {

        @Override
        public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
            // Check if the status code is not success.
            Status status = result.getStatus();
            if (!status.isSuccess()) {
                Log.d(Declarations.TAG_DEBUG, "MatchInitiatedCallback : Erreur : " + status.getStatusCode());
                switch (status.getStatusCode()) {
                    case GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_INVALID_OPERATION:
                        Log.d(Declarations.TAG_DEBUG, "MatchInitiatedCallback : Erreur : STATUS_MULTIPLAYER_ERROR_INVALID_OPERATION");
                        break;
                }
                return;
            }
            TurnBasedMatch match = result.getMatch();
            // If this player is not the first player in this match, continue.
            if (match.getData() != null) {
                return;
            }
            // Otherwise, this is the first player. Initialize the game state.
//            initGame(match);
            hasard = new Random();
            for (Participant par : match.getParticipants()) {
                String idplayer = par.getPlayer().getPlayerId();
                for (DonneesJoueur dj : mPlayerData) {
                    if (dj.getIdentifiantJoueur().equals(idplayer)) {
                        int idx = mPlayerData.indexOf(dj);
                        dj.setIdentifiantParticipant(par.getParticipantId());
                        mPlayerData.set(idx, dj);
                    }
                }
            }
            // Let the player take the first turn
            Games.TurnBasedMultiplayer.registerMatchUpdateListener(mGoogleApiClient, ActiviteTVAccueil.this);
            mMatchId = match.getMatchId();
            showTurnUI(mPlayerData.get(0).getIdentifiantJoueur());
            int josu;
            if (mPlayerData.size() == 1)
                josu = 0;
            else
                josu = 1;
            Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.getMatchId(), DonneesJoueur.tojsonliste(mPlayerData).getBytes(),
                    mPlayerData.get(josu).getIdentifiantParticipant())
                    .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                        @Override
                        public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
                            Log.d(Declarations.TAG_DEBUG, "Games.TurnBasedMultiplayer.takeTurn : updateMatchResult succès : "
                                    + updateMatchResult.getStatus().isSuccess());
                        }
                    });
        }
    }

    private void showTurnUI(String idjoueur) {
//        String participantcourant = match.getPendingParticipantId();
//        Participant par = match.getParticipant(participantcourant);
        Log.d(Declarations.TAG_DEBUG, "showTurnUI : joueur : " + idjoueur);
        int jindex = 0;
        for (DonneesJoueur ji : mPlayerData) {
            Log.d(Declarations.TAG_DEBUG, "showTurnUI : joueur : " + ji.getNomComplet() + " : penalite : " + ji.getTourspenalite());
            if (idjoueur.equals(ji.getIdentifiantJoueur())) {
                jindex = mPlayerData.indexOf(ji);
            }
        }
        Log.d(Declarations.TAG_DEBUG, "showTurnUI : penalite joueur : " + mPlayerData.get(jindex).getTourspenalite());
        if (mPlayerData.get(jindex).getTourspenalite() == 0) {
            int coup = hasard.nextInt(6) + 1;
            tvde.setText(String.valueOf(coup));
            tvde.setVisibility(View.VISIBLE);
            speakOut(mPlayerData.get(jindex).getPrenom() + " " + String.valueOf(coup));
            deplacerPion(jindex, coup, true, false);
        } else {
            speakOut(mPlayerData.get(jindex).getPrenom() + " " + getResources().getString(R.string.textepasser));
            DonneesJoueur donjou = mPlayerData.get(jindex);
            int penalite = donjou.getTourspenalite();
            Log.d(Declarations.TAG_DEBUG, "showTurnUI : penalite joueur : " + jindex + " : penalite initiale : " + penalite);
            donjou.setTourspenalite(penalite - 1);
            mPlayerData.set(jindex, donjou);
            Log.d(Declarations.TAG_DEBUG, "showTurnUI : penalite joueur : " + jindex + " : penalite finale : " + mPlayerData.get(jindex).getTourspenalite());
            if (mMatchId != null) {
                tourSuivant(jindex, donjou);
            } else {
                btntour.setVisibility(View.VISIBLE);
            }
        }
    }

    private void deplacerPion(final int jj, final int valeurde, final boolean premier, final boolean encore) {
        final DonneesJoueur dj = mPlayerData.get(jj);
        Log.d(Declarations.TAG_DEBUG, "deplacerPion : index joueur : " + jj + " : position initiale : " + dj.getPosition()
                + " : penalite : " + dj.getTourspenalite());
        View pion = getPionJoueur(dj.getNumero());
        pion.setElevation(10.0f);
        View depart = getVueCase(dj.getPosition());
        if (depart == null) {
            depart = pion;
        }
        int posit = dj.getPosition() + valeurde;
        if (posit > 63) {
            posit = 63 - (posit - 63);
        }
        View arrivee = getVueCase(posit);
        dj.setPosition(posit);
        mPlayerData.set(jj, dj);
        Log.d(Declarations.TAG_DEBUG, "deplacerPion : index joueur : " + jj + " : position finale : " + dj.getPosition());
        int piongauche = pion.getLeft();
        int pionhaut = pion.getTop();
        int departgauche = depart.getLeft();
        int departhaut = depart.getTop();
        int arriveegauche = arrivee.getLeft();
        int arriveehaut = arrivee.getTop();
        TranslateAnimation anim = new TranslateAnimation(departgauche - piongauche, arriveegauche - piongauche,
                departhaut - pionhaut, arriveehaut - pionhaut);
        anim.setDuration(1000);
        anim.setFillAfter(true);
//        anim.setFillEnabled(true);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                tvde.setVisibility(View.INVISIBLE);
                if (encore) {
                    Log.d(Declarations.TAG_DEBUG, "deplacerPion : index joueur : " + jj + " : rejouer");
                    showTurnUI(mPlayerData.get(jj).getIdentifiantJoueur());
                } else {
                    int decalage = 0;
                    boolean rejouer = false;
                    if (premier) {
                        Log.d(Declarations.TAG_DEBUG, "deplacerPion : index joueur : " + jj + " : premier : " + premier);
                        if (dj.getPosition() == 5) {
                            speakOut(getResources().getString(R.string.texteoie));
                            decalage = 4;
                        } else if (dj.getPosition() == 6) {
                            speakOut(getResources().getString(R.string.textepont));
                            decalage = 6;
                            rejouer = true;
                        } else if (dj.getPosition() == 9) {
                            speakOut(getResources().getString(R.string.texteoie));
                            decalage = 5;
                        } else if (dj.getPosition() == 12) {
                            speakOut(getResources().getString(R.string.textepont));
                            decalage = -6;
                            rejouer = true;
                        } else if (dj.getPosition() == 14) {
                            speakOut(getResources().getString(R.string.texteoie));
                            decalage = 4;
                        } else if (dj.getPosition() == 18) {
                            speakOut(getResources().getString(R.string.texteoie));
                            decalage = 5;
                        } else if (dj.getPosition() == 19) {
                            speakOut(getResources().getString(R.string.texteauberge));
                            dj.setTourspenalite(2);
                            mPlayerData.set(jj, dj);
                        } else if (dj.getPosition() == 23) {
                            speakOut(getResources().getString(R.string.texteoie));
                            decalage = 4;
                        } else if (dj.getPosition() == 26) {
                            speakOut(getResources().getString(R.string.textedes));
                            decalage = 27;
                            rejouer = true;
                        } else if (dj.getPosition() == 27) {
                            speakOut(getResources().getString(R.string.texteoie));
                            decalage = 5;
                        } else if (dj.getPosition() == 31) {
                            speakOut(getResources().getString(R.string.textepuit));
                            dj.setTourspenalite(3);
                            mPlayerData.set(jj, dj);
                        } else if (dj.getPosition() == 32) {
                            speakOut(getResources().getString(R.string.texteoie));
                            decalage = 4;
                        } else if (dj.getPosition() == 36) {
                            speakOut(getResources().getString(R.string.texteoie));
                            decalage = 5;
                        } else if (dj.getPosition() == 41) {
                            speakOut(getResources().getString(R.string.texteoie));
                            decalage = 4;
                        } else if (dj.getPosition() == 42) {
                            speakOut(getResources().getString(R.string.textelabyrinthe));
                            decalage = -12;
                            dj.setTourspenalite(1);
                            mPlayerData.set(jj, dj);
                        } else if (dj.getPosition() == 45) {
                            speakOut(getResources().getString(R.string.texteoie));
                            decalage = 5;
                        } else if (dj.getPosition() == 50) {
                            speakOut(getResources().getString(R.string.texteoie));
                            decalage = 4;
                        } else if (dj.getPosition() == 52) {
                            speakOut(getResources().getString(R.string.texteprison));
                            dj.setTourspenalite(3);
                            mPlayerData.set(jj, dj);
                        } else if (dj.getPosition() == 53) {
                            speakOut(getResources().getString(R.string.textedes));
                            decalage = -27;
                            rejouer = true;
                        } else if (dj.getPosition() == 54) {
                            speakOut(getResources().getString(R.string.texteoie));
                            decalage = 5;
                        } else if (dj.getPosition() == 58) {
                            speakOut(getResources().getString(R.string.textetetemort));
                            decalage = -57;
                        }
                    }
                    if (decalage != 0) {
                        Log.d(Declarations.TAG_DEBUG, "deplacerPion : index joueur : " + jj + " : decalage : " + decalage);
                        envoyerPosition(jj, null);
                        deplacerPion(jj, decalage, false, rejouer);
                    } else {
                        if (mMatchId != null) {
                            tourSuivant(jj, dj);
                        } else {
                            if (dj.getPosition() != 63) {
                                btntour.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
                Log.d(Declarations.TAG_DEBUG, "deplacerPion : fin animation : index joueur : " + jj + " : penalite : " + mPlayerData.get(jj).getTourspenalite());
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        pion.startAnimation(anim);
    }

    private String trouverParticipantSuivant(int idxactuel) {
        Log.d(Declarations.TAG_DEBUG, "trouverParticipantSuivant : index joueur : " + idxactuel);
        int idjsuivant = idxactuel + 1;
        if (idjsuivant >= mPlayerData.size()) {
            idjsuivant = 0;
        }
/*        int sanction = mPlayerData.get(idjsuivant).getTourspenalite();
        if (sanction > 0) {
            mPlayerData.get(idjsuivant).setTourspenalite(sanction - 1);
            idjsuivant = idxactuel + 1;
            if (idjsuivant >= mPlayerData.size()) {
                idjsuivant = 0;
            }
        }*/
        Log.d(Declarations.TAG_DEBUG, "trouverParticipantSuivant : index index joueur suivant : " + idjsuivant);
        String parsuivant = mPlayerData.get(idjsuivant).getIdentifiantParticipant();
        Log.d(Declarations.TAG_DEBUG, "trouverParticipantSuivant : participant suivant : " + parsuivant);
        return parsuivant;
    }

    private String trouverParticipantSuivant(String idjoueuractuel) {
        Log.d(Declarations.TAG_DEBUG, "trouverParticipantSuivant : joueur : " + idjoueuractuel);
        int jindex = 0;
        for (DonneesJoueur ji : mPlayerData) {
            if (idjoueuractuel.equals(ji.getIdentifiantJoueur())) {
                jindex = mPlayerData.indexOf(ji);
            }
        }
        return trouverParticipantSuivant(jindex);
    }

    private void tourSuivant(int indexj, DonneesJoueur jou) {
        Log.d(Declarations.TAG_DEBUG, "tourSuivant : index joueur : " + indexj);
        if (indexj == 0) {
            String pars = trouverParticipantSuivant(indexj);
            if (jou.getPosition() != 63) {
                Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatchId,
                        DonneesJoueur.tojsonliste(mPlayerData).getBytes(), pars)
                        .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                            @Override
                            public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
                                Log.d(Declarations.TAG_DEBUG, "Games.TurnBasedMultiplayer.takeTurn : succès : "
                                        + updateMatchResult.getStatus().isSuccess());
                            }
                        });
            } else {
                List<ParticipantResult> resultats = new ArrayList<>();
                for (DonneesJoueur dj : mPlayerData) {
                    ParticipantResult res;
                    int idxj = mPlayerData.indexOf(dj);
                    if (idxj == 0) {
                        res = new ParticipantResult(dj.getIdentifiantParticipant(),
                                ParticipantResult.MATCH_RESULT_WIN, ParticipantResult.PLACING_UNINITIALIZED);
                        dj.setGagnant(true);
                    } else {
                        res = new ParticipantResult(dj.getIdentifiantParticipant(),
                                ParticipantResult.MATCH_RESULT_LOSS, ParticipantResult.PLACING_UNINITIALIZED);
                        dj.setGagnant(false);
                    }
                    resultats.add(res);
                    mPlayerData.set(idxj, dj);
                }
                Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, mMatchId,
                        DonneesJoueur.tojsonliste(mPlayerData).getBytes(), resultats)
                        .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                            @Override
                            public void onResult(TurnBasedMultiplayer.UpdateMatchResult updateMatchResult) {
                                Log.d(Declarations.TAG_DEBUG, "Games.TurnBasedMultiplayer.finishMatch : succès : "
                                        + updateMatchResult.getStatus().isSuccess());
                            }
                        });
                Intent intent = new Intent(ActiviteTVAccueil.this, ActiviteTVResultats.class);
                intent.putExtra(Declarations.TAG_JSON_DONNEES, DonneesJoueur.tojsonliste(mPlayerData));
                startActivity(intent);
            }
        } else {
            Log.d(Declarations.TAG_DEBUG, "tourSuivant : index joueur : " + indexj + " : envoi position");
            envoyerPosition(indexj, trouverParticipantSuivant(indexj));
        }
    }

    private void envoyerPosition(int idxj, String joueursuivant) {
        Log.d(Declarations.TAG_DEBUG, "envoyerPosition : index joueur : " + idxj + " : joueur suivant : " + joueursuivant);
        String remote = mPlayerData.get(idxj).getRemoteid();
        JSONObject joreponse = new JSONObject();
        try {
            joreponse.put(Declarations.TAG_JSON_ENTETE, Declarations.REPONSE_LANCER_DE);
            joreponse.put(Declarations.TAG_JSON_DONNEES, DonneesJoueur.tojsonliste(mPlayerData).toString());
            if (joueursuivant != null)
                joreponse.put(Declarations.TAG_JSON_JOUEUR, joueursuivant);
            Nearby.Connections.sendReliableMessage(mGoogleApiClient, remote, joreponse.toString().getBytes());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTurnBasedMatchReceived(TurnBasedMatch turnBasedMatch) {
        String donnees = new String(turnBasedMatch.getData());
//        Participant par = turnBasedMatch.getParticipant(turnBasedMatch.getPendingParticipantId());
        Log.d(Declarations.TAG_DEBUG, "mGoogleApiClient : onTurnBasedMatchReceived : donnees : " + donnees);
        if (turnBasedMatch.getStatus() == TurnBasedMatch.MATCH_STATUS_ACTIVE) {
            Log.d(Declarations.TAG_DEBUG, "onTurnBasedMatchReceived : MATCH_STATUS_ACTIVE");
            if (turnBasedMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
                Log.d(Declarations.TAG_DEBUG, "onTurnBasedMatchReceived : MY TURN");
                mMatchId = turnBasedMatch.getMatchId();
                speakOut(getResources().getString(R.string.amontour));
                btntour.setVisibility(View.VISIBLE);
            } else {
                Log.d(Declarations.TAG_DEBUG, "onTurnBasedMatchReceived : NOT MY TURN");
//            showTurnUI(par.getPlayer().getPlayerId());
            }
        } else if (turnBasedMatch.getStatus() == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
            Log.d(Declarations.TAG_DEBUG, "onTurnBasedMatchReceived : MATCH_STATUS_COMPLETE");
            Intent intent = new Intent(this, ActiviteTVResultats.class);
            intent.putExtra(Declarations.TAG_JSON_DONNEES, donnees);
            startActivity(intent);
        }
    }

    @Override
    public void onTurnBasedMatchRemoved(String s) {


    }

}