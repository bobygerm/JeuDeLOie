package debut.jeudeloie.commun;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class DonneesJoueur {

    private static final String TAG_JOUEUR_ID = "JOUEUR_ID";
    private static final String TAG_PARTICIPANT_ID = "PARTICIPANT_ID";
    private static final String TAG_JOUEUR_NUMERO = "JOUEUR_NUMERO";
    private static final String TAG_NOM_COMPLET = "JOUEUR_NOM_COMPLET";
    private static final String TAG_PRENOM = "JOUEUR_PRENOM";
    private static final String TAG_URL_LOGO = "JOUEUR_URL_LOGO";
    private static final String TAG_JOUEUR_POSITION = "JOUEUR_POSITION";
    private static final String TAG_JOUEUR_GAGNANT = "JOUEUR_GAGNANT";
    private static final String TAG_PENALITE = "JOUEUR_PENALITE";


    private String idJoueur;
    private String idParticipant;
    private int numero;
    private String nomcomplet;
    private String prenom;
    private String urllogo;
    private int position;
    private String remoteid;
    private int tourspenalite;
    private boolean gagnant;

    public String getIdentifiantJoueur() {
        return idJoueur;
    }

    public void setIdentifiantJoueur(String idj) {
        this.idJoueur = idj;
    }

    public String getIdentifiantParticipant() {
        return idParticipant;
    }

    public void setIdentifiantParticipant(String idj) {
        this.idParticipant = idj;
    }

    public String getNomComplet() {
        return nomcomplet;
    }

    public void setNomComplet(String nomcomplet) {
        this.nomcomplet = nomcomplet;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getUrllogo() {
        return urllogo;
    }

    public void setUrllogo(String urllogo) {
        this.urllogo = urllogo;
    }

    public JSONObject getjson() {
        JSONObject retour = null;
        try {
            retour = new JSONObject();
            retour.put(TAG_JOUEUR_ID, idJoueur);
            retour.put(TAG_PARTICIPANT_ID, idParticipant);
            retour.put(TAG_JOUEUR_NUMERO, numero);
            retour.put(TAG_NOM_COMPLET, nomcomplet);
            retour.put(TAG_PRENOM, prenom);
            retour.put(TAG_URL_LOGO, urllogo);
            retour.put(TAG_JOUEUR_POSITION, position);
            retour.put(TAG_JOUEUR_GAGNANT, gagnant);
            retour.put(TAG_PENALITE, tourspenalite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return retour;
    }

    public static DonneesJoueur fromjson(String don) {
        DonneesJoueur retour = null;
        try {
            JSONObject json = new JSONObject(don);
            retour = new DonneesJoueur();
            retour.setIdentifiantJoueur(json.getString(TAG_JOUEUR_ID));
            if (json.has(TAG_PARTICIPANT_ID))
                retour.setIdentifiantParticipant(json.getString(TAG_PARTICIPANT_ID));
            retour.setNumero(json.getInt(TAG_JOUEUR_NUMERO));
            retour.setNomComplet(json.getString(TAG_NOM_COMPLET));
            retour.setPrenom(json.getString(TAG_PRENOM));
            retour.setUrllogo(json.getString(TAG_URL_LOGO));
            retour.setPosition(json.getInt(TAG_JOUEUR_POSITION));
            retour.setGagnant(json.getBoolean(TAG_JOUEUR_GAGNANT));
            retour.setTourspenalite(json.getInt(TAG_PENALITE));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return retour;
    }

    public static ArrayList<DonneesJoueur> fromjsonliste(String don) {
        ArrayList<DonneesJoueur> retour = null;
        try {
            JSONArray json = new JSONArray(don);
            retour = new ArrayList<>();
            for (int i = 0; i < json.length(); i++) {
                JSONObject dj = (JSONObject) json.get(i);
                DonneesJoueur djor = DonneesJoueur.fromjson(dj.toString());
                retour.add(djor);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return retour;
    }

    public static String tojsonliste(ArrayList<DonneesJoueur> liste) {
        JSONArray jlis = new JSONArray();
        for (DonneesJoueur dj : liste) {
            JSONObject jo = dj.getjson();
            jlis.put(jo);
        }
        return jlis.toString();
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getRemoteid() {
        return remoteid;
    }

    public void setRemoteid(String remoteid) {
        this.remoteid = remoteid;
    }

    public int getTourspenalite() {
        return tourspenalite;
    }

    public void setTourspenalite(int penalite) {
        this.tourspenalite = penalite;
    }

    public boolean isGagnant() {
        return gagnant;
    }

    public void setGagnant(boolean gagnant) {
        this.gagnant = gagnant;
    }
}
