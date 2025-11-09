package ma.emsi.gounidimeryem.tp4_meryemgounidi_web.jsf;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("bb")
@ViewScoped
public class Bb implements Serializable {

    @Inject
    private LlmClientGemini llmClient;

    @Inject
    private FacesContext facesContext;

    private String roleSysteme;
    private boolean roleSystemeChangeable = true;
    private List<SelectItem> rolesSysteme;  // renommé pour matcher index.xhtml

    public List<SelectItem> getRolesSysteme() {  // nom attendu par index.xhtml
        if (rolesSysteme == null) {
            rolesSysteme = new ArrayList<>();
            String assistant = "You are a helpful assistant. Answer clearly and concisely.";
            rolesSysteme.add(new SelectItem(assistant, "Assistant"));
            String translator = "You translate between English and French with examples.";
            rolesSysteme.add(new SelectItem(translator, "Traducteur"));
        }
        return rolesSysteme;
    }

    private String question;
    private String reponse;
    private final StringBuilder conversation = new StringBuilder();

    public void envoyer() {
        if (question == null || question.isBlank()) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Question vide", null));
            return;
        }
        if (roleSystemeChangeable && roleSysteme != null && !roleSysteme.isBlank()) {
            llmClient.setSystemRole(roleSysteme);
            roleSystemeChangeable = false;
        }
        reponse = llmClient.chat(question);
        conversation.append("User : ").append(question).append("\n")
                .append("Assistant : ").append(reponse).append("\n\n");
        question = "";
    }

    public void nouveauChat() {
        conversation.setLength(0);
        reponse = "";
        question = "";
        roleSystemeChangeable = true;
        llmClient.reset();
    }

    public String getRoleSysteme() { return roleSysteme; }
    public void setRoleSysteme(String roleSysteme) { this.roleSysteme = roleSysteme; }
    public boolean isRoleSystemeChangeable() { return roleSystemeChangeable; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getReponse() { return reponse; }
    public void setReponse(String reponse) { this.reponse = reponse; }
    public String getConversation() { return conversation.toString(); }
}

