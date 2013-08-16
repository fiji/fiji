package vib.app.module;

public interface ModuleListener {
	public void moduleFinished(Module module, int index);
	public void exceptionOccurred(Module module, int index);
}
