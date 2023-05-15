package it.polito.dsp.ClientExceptions;

public class ServerFailureException extends Exception {

	public enum KindOfFailure{
		DURING_RECOVERY,
		DURING_FIRST_INTERACTION,
		DURING_TRANSFER
	}
	
	private final KindOfFailure kindOfFailure;
	
	public ServerFailureException(KindOfFailure kindOfFailure) {
		this.kindOfFailure = kindOfFailure;
	}

	public KindOfFailure getKindOfFailure() {
		return kindOfFailure;
	}

}
