package mpicbg.spim.io;

public class ConfigurationParserObject
{
	protected String entryName;
	protected String dataType;
	protected String variableName;
	protected int variableFieldPosition;
	
	public ConfigurationParserObject() 
	{
		entryName = dataType = variableName = "";
		variableFieldPosition = -1;
	}
	
	public ConfigurationParserObject( String entryName, String dataType, String variableName, int variableFieldPosition )
	{
		this.entryName = entryName;
		this.dataType = dataType;
		this.variableName = variableName;
		this.variableFieldPosition = variableFieldPosition;
	}
		
	public String getEntry() { return entryName; }
	public String getDataType() { return dataType; }
	public String getVariableName() { return variableName;	}
	public int getVariableFieldPosition() { return variableFieldPosition; }

	public void setEntry( final String entryName ) { this.entryName = entryName; }
	public void setDataType( final String dataType ) { this.dataType = dataType; }
	public void setVariableName( final String variableName ) { this.variableName = variableName; }
	public void setVariableFieldPosition( final int variableFieldPosition ) { this.variableFieldPosition = variableFieldPosition; }
	
}
