(ns llama.test.state
  (:use llama.state
        [lazytest.describe :only [describe testing it]]))

(describe "states"
  (testing save-state
    (it "should save the state"
        (do (save-state :test-state [1 2 3])
            true)))
  (testing load-state
    (it "should load the same state"
        (= [1 2 3]
           (load-state :test-state))))
  (testing defstate
    (it "should define a state"
        (defstate :test-state (fn [] {:a 3 :b 4}))))
  (testing save-bean-states
    (it "should save the defined states"
        (do (save-states)
            true))
    (it "should load the same state"
        (= (load-state :test-state)
           {:a 3 :b 4}))))

(describe "beans"
  (testing save-bean 
    (it "should save the bean"
        (do (save-bean :test-bean java.awt.Color/white)
            true)))
  (testing load-bean
    (it "should load the same bean"
        (= (load-bean :test-bean)
           java.awt.Color/white)))
  (testing defbean
    (it "should define a bean"
        (defbean :test-bean (fn [] java.awt.Color/green))))
  (testing save-bean-states
    (it "should save the defined beans"
        (do (save-bean-states)
            true))
    (it "should load the same bean"
        (= (load-bean :test-bean)
           java.awt.Color/green))))
