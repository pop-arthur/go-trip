object TripConfig {
  val appName: String = "Go Trip"

  def welcomeMessage(): String =
    s"Welcome to $appName!"
}

@main
def main(): Unit = {
  println(TripConfig.welcomeMessage())
}
