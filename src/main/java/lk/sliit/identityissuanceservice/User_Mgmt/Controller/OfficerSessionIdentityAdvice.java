package lk.sliit.identityissuanceservice.User_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class OfficerSessionIdentityAdvice {

    @ModelAttribute
    public void exposeOfficerIdentity(HttpSession session, Model model) {
        copySessionAttribute(session, model, "loggedInOfficerEmail");
        copySessionAttribute(session, model, "loggedInOfficerRole");
        copySessionAttribute(session, model, "loggedInOfficerRoleLabel");
        copySessionAttribute(session, model, "loggedInOfficerEmployeeNo");
        copySessionAttribute(session, model, "loggedInOfficerLoginTime");
    }

    private void copySessionAttribute(HttpSession session, Model model, String attributeName) {
        Object value = session.getAttribute(attributeName);
        if (value != null) {
            model.addAttribute(attributeName, value);
        }
    }
}
