package com.mckevin.agentcrm.application.port;

/** Le CRM est indisponible (erreur, délai dépassé ou circuit ouvert). */
public class CrmUnavailableException extends RuntimeException {

    public CrmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
