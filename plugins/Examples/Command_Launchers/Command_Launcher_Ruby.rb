include_class 'java.awt.Color'
include_class 'java.awt.event.TextListener'
 
class TypeListener
 
  # This is the (slightly surprising) JRuby way of implementing
  # a Java interface:
  include TextListener
 
  def initialize(commands,prompt)
    @commands = commands
    @prompt = prompt
  end
 
  def textValueChanged(tvc)
    text = @prompt.getText
    if @commands.include? text
      @prompt.setForeground Color.black
    else
      @prompt.setForeground Color.red
    end
  end
 
end
 
commands = ij.Menus.getCommands.keySet.toArray
 
gd = ij.gui.GenericDialog.new 'CommandLauncher'
gd.addStringField 'Command: ', ''
 
prompt = gd.getStringFields[0]
prompt.setForeground Color.red
 
prompt.addTextListener TypeListener.new( commands, prompt )
 
gd.showDialog
unless gd.wasCanceled
  ij.IJ.doCommand gd.getNextString
end
