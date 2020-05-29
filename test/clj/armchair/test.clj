(ns armchair.test
  (:require [clojure.test :refer [run-all-tests deftest testing]])
  (:import [org.openqa.selenium By]
           [org.openqa.selenium.chrome ChromeDriver ChromeOptions]
           [org.openqa.selenium.support.ui WebDriverWait ExpectedConditions]))

(System/setProperty "webdriver.chrome.driver" "dev-resources/chromedriver")

(deftest basics
  (testing "chrome driver"
    (let [driver (ChromeDriver. (doto (ChromeOptions.)
                                  (.addArguments ["--headless"])))
          wait (WebDriverWait. driver 10)]
      (try
        (.get driver "http://localhost:3449/")
        (.until wait (ExpectedConditions/presenceOfElementLocated (By/cssSelector "#app")))
        (finally
          (.quit driver))))))

(defn main []
  (run-all-tests))
