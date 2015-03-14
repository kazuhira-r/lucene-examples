def partOfSpeechCsv = args[0]
def inputCsv = args[1]
def outputCsv = args[2]

def maxContext = 1

def partOfSpeechMap = [:]
new File(partOfSpeechCsv).eachLine { line ->
  def tokens = line.split(/,/)
  def contexts = [tokens[0], tokens[1]]
  def partOfSpeech = tokens.drop(2).join('-')
  partOfSpeechMap[partOfSpeech] = contexts

  if ((tokens[0] as int) > maxContext) {
    maxContext = tokens[0] as int
  }
}

new File(outputCsv).withWriter('UTF-8') { writer ->
  new File(inputCsv).eachLine('UTF-8') { line ->
    def tokens = line.split(/,/)

    if (tokens[10].length() >= 16) {
      println("[WARN] Discard, BaseForm length greather than 16. => [${tokens[10]}]")
      return
    }
   
    def contexts = [tokens[1], tokens[2]]
    def partOfSpeech = "${tokens[4]}-${tokens[5]}-${tokens[6]}-${tokens[7]}-${tokens[8]}"

    def leftContext
    def rightContext

    def ipadicContext = partOfSpeechMap[partOfSpeech]
    if (ipadicContext == null) {
      maxContext++
      leftContext = maxContext
      rightContext = maxContext
      partOfSpeechMap[partOfSpeech] = [leftContext as String, rightContext as String]
    } else if (ipadicContext != contexts) {
      leftContext = ipadicContext[0] as int
      rightContext = ipadicContext[1] as int
    } else {
      leftContext = contexts[0] as int
      rightContext = contexts[1] as int
    }

    writer.write(tokens[0])
    writer.write(',')
    writer.write(leftContext as String)
    writer.write(',')
    writer.write(rightContext as String)
    writer.write(',')

    writer.write(tokens.drop(3).join(','))
    writer.newLine()
  }
}
