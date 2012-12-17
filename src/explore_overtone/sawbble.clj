(ns explore-overtone.sawbble
  (:use [overtone.live]
        [overtone.gui.control]
        [explore-overtone.scope]))

;; Work step-by-step to create a saw synth

;; ======================================================================
;; watch what we synthesize on an oscilloscope
(scope :width 700 :height 500) 

;; ======================================================================
;; Step 1 - the basic synth 
;;
(defsynth saw-synth-1
  "a basic saw synth"
  [pitch-midi         {:default 60  :min 40   :max 70  :step 1}
   gate               {:default 1.0 :min 0.0  :max 1.0 :step 1}]
  (let [pitch-freq (midicps pitch-midi)
        saw-out (saw pitch-freq)]
    (out 0 (pan2 (* gate saw-out)))))

;; play with the synth (window may "pop under")
;; toggle the "gate" control to turn a note on & off
(live-synth-controller saw-synth-1)

;; ======================================================================
;; Step 2 - use better scale than chromatic, add envelope
;;
;; NOTE - to get this working with the GUI controller, I remove an
;; important bit of code from the env-gen call: :action FREE.  For a
;; "real" synth, you will want it to disappear wen the envelope goes
;; to 0.
;;

;; create a buffer to hold the scale
(def scale-buffer (buffer 16))
;; fill it with a scale
(doseq [[i n] (map-indexed
               vector
               (scale :d2 :minor (range 1 16)))]
  (buffer-set! scale-buffer i n))

;; synth
(defsynth saw-synth-2
  "a basic saw synth with an envelope"
  [pitch-index        {:default 0   :min 0   :max 15  :step 1}
   adsr-attack-time   {:default 0.1 :min 0.0 :max 1.0 :step 0.01}
   adsr-decay-time    {:default 0.1 :min 0.0 :max 1.0 :step 0.01}
   adsr-sustain-level {:default 0.5 :min 0.0 :max 1.0 :step 0.01}
   adsr-release-time  {:default 0.1 :min 0.0 :max 1.0 :step 0.01}
   adsr-peak-level    {:default 0.9 :min 0.0 :max 1.0 :step 0.01}
   adsr-curve         {:default -4  :min -5  :max 5   :step 1} ;; what are valid values here?
   gate               {:default 1.0 :min 0.0 :max 1.0 :step 1}]
  (let [pitch-midi (index:kr (:id scale-buffer) pitch-index)
        pitch-freq (midicps pitch-midi)
        saw-out (saw pitch-freq)
        env-out (env-gen (adsr adsr-attack-time   adsr-decay-time
                               adsr-sustain-level adsr-release-time
                               adsr-peak-level    adsr-curve)
                         :gate gate)]
    (out 0 (pan2 (* env-out saw-out)))))

;; play with the synth & scale
(live-synth-controller saw-synth-2)

;; ======================================================================
;; Step 3 - add detuned 2nd frequency
;;
;; this adds a bit of "chorus" or depth to the synth
;;
(defsynth saw-synth-3
  "a detuned saw synth with an envelope"
  [lfo-level          {:default 1.4 :min 0.0  :max 5.0  :step 0.05}
   lfo-freq           {:default 1.8 :min 0.0  :max 10.0 :step 0.1}
   pitch-index        {:default 0   :min 0    :max 15   :step 1}
   adsr-attack-time   {:default 0.1 :min 0.0  :max 1.0  :step 0.01}
   adsr-decay-time    {:default 0.1 :min 0.0  :max 1.0  :step 0.01}
   adsr-sustain-level {:default 0.5 :min 0.0  :max 1.0  :step 0.01}
   adsr-release-time  {:default 0.1 :min 0.0  :max 1.0  :step 0.01}
   adsr-peak-level    {:default 0.9 :min 0.0  :max 1.0  :step 0.01}
   adsr-curve         {:default -4  :min -5   :max 5    :step 1}
   gate               {:default 1.0 :min 0.0  :max 1.0  :step 1}]
  (let [pitch-midi (index:kr (:id scale-buffer) pitch-index)
        pitch-freq (midicps pitch-midi)
        lfo-out (* lfo-level (sin-osc lfo-freq))
        saws-out (mix (saw [pitch-freq (+ pitch-freq lfo-out)])) ;; use same saw
        env-out (env-gen (adsr adsr-attack-time   adsr-decay-time
                               adsr-sustain-level adsr-release-time
                               adsr-peak-level    adsr-curve)
                         :gate gate)]
    (out 0 (pan2 (* env-out saws-out)))))

;; play with the synth, scale & detuning (lfo-level & freq)
(live-synth-controller saw-synth-3)

;; ======================================================================
;; Step 4 - add separation delay between the stereo channels
;;
;; this adds a "width" to the stero field
;;
(defsynth saw-synth-4
  "a detuned and stereo-separated saw synth with an envelope"  
  [separation-delay-ms {:default 5.0 :min 0    :max 30.0 :step 0.1}
   right-phase         {:default 1   :min -1   :max 1    :step 2}
   lfo-level           {:default 1.4 :min 0.0  :max 5.0  :step 0.05}
   lfo-freq            {:default 1.8 :min 0.0  :max 10.0 :step 0.1}
   pitch-index         {:default 0   :min 0    :max 15   :step 1}
   adsr-attack-time    {:default 0.1 :min 0.0  :max 1.0  :step 0.01}
   adsr-decay-time     {:default 0.1 :min 0.0  :max 1.0  :step 0.01}
   adsr-sustain-level  {:default 0.5 :min 0.0  :max 1.0  :step 0.01}
   adsr-release-time   {:default 0.1 :min 0.0  :max 1.0  :step 0.01}
   adsr-peak-level     {:default 0.9 :min 0.0  :max 1.0  :step 0.01}
   adsr-curve          {:default -4  :min -5   :max 5    :step 1}
   gate                {:default 1.0 :min 0.0  :max 1.0  :step 1}]
  (let [pitch-midi (index:kr (:id scale-buffer) pitch-index)
        pitch-freq (midicps pitch-midi)
        lfo-out (* lfo-level (sin-osc lfo-freq))
        saws-out (mix (saw [pitch-freq (+ pitch-freq lfo-out)]))
        separation-delay (/ separation-delay-ms 1000.0)
        saws-out-2ch [saws-out
                      (delay-c (* right-phase saws-out)
                               1.0 separation-delay)]
        env-out (env-gen (adsr adsr-attack-time   adsr-decay-time
                               adsr-sustain-level adsr-release-time
                               adsr-peak-level    adsr-curve)
                         :gate gate)]
    ;; NOTE -- no more pan2 as the saws are now in stereo
    (out 0 (* env-out saws-out-2ch))))

;;  play with the stereo separation
(live-synth-controller saw-synth-4)

;; ======================================================================
;; Step 5 - add low-pass filter to the output
;;
;; this makes it a much more pleasant sound without some of the
;; upper-end harshness
;;
(defsynth saw-synth-5
  "a detuned and stereo-separated saw synth with a low-pass-filter."  
  [lpf-freq            {:default 3000 :min 100  :max 10000 :step 100}
   lpf-res             {:default 0.1  :min 0.0  :max 1.0   :step 0.05}
   separation-delay-ms {:default 5.0  :min 0    :max 30.0  :step 0.1}
   right-phase         {:default 1    :min -1   :max 1     :step 2}
   lfo-level           {:default 1.4  :min 0.0  :max 5.0   :step 0.05}
   lfo-freq            {:default 1.8  :min 0.0  :max 10.0  :step 0.1}
   pitch-index         {:default 0    :min 0    :max 15    :step 1}
   adsr-attack-time    {:default 0.1  :min 0.0  :max 1.0   :step 0.01}
   adsr-decay-time     {:default 0.1  :min 0.0  :max 1.0   :step 0.01}
   adsr-sustain-level  {:default 0.5  :min 0.0  :max 1.0   :step 0.01}
   adsr-release-time   {:default 0.1  :min 0.0  :max 1.0   :step 0.01}
   adsr-peak-level     {:default 0.9  :min 0.0  :max 1.0   :step 0.01}
   adsr-curve          {:default -4   :min -5   :max 5     :step 1}
   gate                {:default 1.0  :min 0.0  :max 1.0   :step 1}]
  (let [pitch-midi (index:kr (:id scale-buffer) pitch-index)
        pitch-freq (midicps pitch-midi)
        lfo-out (* lfo-level (sin-osc lfo-freq))
        saws-out (mix (saw [pitch-freq (+ pitch-freq lfo-out)]))
        separation-delay (/ separation-delay-ms 1000.0)
        saws-out-2ch [saws-out (delay-c (* right-phase saws-out)
                                        1.0 separation-delay)]
        lpf-out-2ch (moog-ff saws-out-2ch lpf-freq lpf-res)
        env-out (env-gen (adsr adsr-attack-time   adsr-decay-time
                               adsr-sustain-level adsr-release-time
                               adsr-peak-level    adsr-curve)
                         :gate gate)]
    (out 0 (* env-out lpf-out-2ch))))

;; play with the synth & lpf frequency
(live-synth-controller saw-synth-5)

;; ======================================================================
;; Step 6 - add lfo on the low-pass filter
;;
;; gives it yet another type of depth
;;
(defsynth saw-synth-6
  "a detuned and stereo-separated saw synth with a low-pass-filter and
   low-pass-filter LFO."
  [lpf-lfo-freq        {:default 4.1  :min 0.0 :max 10.0  :step 0.01}
   lpf-min-freq        {:default 400  :min 100 :max 9900  :step 100}
   lpf-max-freq        {:default 4000 :min 100 :max 10000 :step 100}
   lpf-res             {:default 0.1  :min 0.0 :max 1.0   :step 0.05}
   separation-delay-ms {:default 5.0  :min 0    :max 30.0  :step 0.1}
   lfo-level           {:default 1.4  :min 0.0 :max 5.0   :step 0.05}
   lfo-freq            {:default 1.8  :min 0.0 :max 10.0  :step 0.1}
   pitch-index         {:default 0    :min 0   :max 15    :step 1}
   adsr-attack-time    {:default 0.1 :min 0.0  :max 1.0   :step 0.01}
   adsr-decay-time     {:default 0.1 :min 0.0  :max 1.0   :step 0.01}
   adsr-sustain-level  {:default 0.5 :min 0.0  :max 1.0   :step 0.01}
   adsr-release-time   {:default 0.1 :min 0.0  :max 1.0   :step 0.01}
   adsr-peak-level     {:default 0.9 :min 0.0  :max 1.0   :step 0.01}
   adsr-curve          {:default -4  :min -5   :max 5     :step 1}
   gate                {:default 1.0 :min 0.0  :max 1.0   :step 1}]
  (let [pitch-midi (index:kr (:id scale-buffer) pitch-index)
        pitch-freq (midicps pitch-midi)
        lfo-out (* lfo-level (sin-osc lfo-freq))
        saws-out (mix (saw [pitch-freq (+ pitch-freq lfo-out)]))
        separation-delay (/ separation-delay-ms 1000.0)
        saws-out-2ch [saws-out (delay-c saws-out 1.0 separation-delay)]
        lpf-freq (lin-lin (sin-osc lpf-lfo-freq) -1 1 lpf-min-freq lpf-max-freq)
        lpf-out-2ch (moog-ff saws-out-2ch lpf-freq lpf-res)
        env-out (env-gen (adsr adsr-attack-time   adsr-decay-time
                               adsr-sustain-level adsr-release-time
                               adsr-peak-level    adsr-curve)
                         :gate gate)]
    (out 0 (* env-out lpf-out-2ch))))

;; play with the synth, scale & lpf-lfo frequency
(live-synth-controller saw-synth-6)

;; ======================================================================
;; The full synth with some added slew on the input notes
(defsynth sawbble-synth
  "a detuned and stereo-separated saw synth with a low-pass-filter and
   low-pass-filter LFO."
  [note                {:default 60   :min 0   :max 127   :step 1}
   ;; adding velocity for (midi-poly-player)
   velocity            {:default 1.0  :min 0.0 :max 10.0  :step 0.1}
   note-slew           {:default 15.0 :min 1.0 :max 50.0  :step 1.0}
   separation-delay-ms {:default 5.0  :min 0   :max 30.0  :step 0.1}
   separation-phase    {:default 1    :min -1  :max 1     :step 2}
   lpf-lfo-freq        {:default 4.1  :min 0.0 :max 10.0  :step 0.01}
   lpf-min-freq        {:default 400  :min 100 :max 9900  :step 100}
   lpf-max-freq        {:default 4000 :min 100 :max 10000 :step 100}
   lpf-res             {:default 0.1  :min 0.0 :max 1.0   :step 0.05}
   lfo-level           {:default 1.4  :min 0.0 :max 5.0   :step 0.05}
   lfo-freq            {:default 1.8  :min 0.0 :max 10.0  :step 0.1}
   adsr-attack-time    {:default 0.1 :min 0.0  :max 1.0   :step 0.01}
   adsr-decay-time     {:default 0.1 :min 0.0  :max 1.0   :step 0.01}
   adsr-sustain-level  {:default 0.5 :min 0.0  :max 1.0   :step 0.01}
   adsr-release-time   {:default 0.1 :min 0.0  :max 1.0   :step 0.01}
   adsr-peak-level     {:default 0.9 :min 0.0  :max 1.0   :step 0.01}
   adsr-curve          {:default -4  :min -5   :max 5     :step 1}
   gate                {:default 1.0 :min 0.0  :max 1.0   :step 1}]
  (let [pitch-midi (slew:kr note note-slew note-slew) ;; interesting lag to changing notes
        pitch-freq (midicps pitch-midi)
        lfo-out (* lfo-level (sin-osc lfo-freq))
        saws-out (mix (saw [pitch-freq (+ pitch-freq lfo-out)]))
        separation-delay (/ separation-delay-ms 1000.0)
        saws-out-2ch [saws-out (delay-c (* separation-phase saws-out)
                                        1.0 separation-delay)]
        lpf-freq (lin-lin (sin-osc lpf-lfo-freq) -1 1 lpf-min-freq lpf-max-freq)
        lpf-out-2ch (moog-ff saws-out-2ch lpf-freq lpf-res)
        env-out (env-gen (adsr adsr-attack-time   adsr-decay-time
                               adsr-sustain-level adsr-release-time
                               adsr-peak-level    adsr-curve)
                         :gate gate :action FREE)] ;; FREE at last!
    (out 0 (* velocity env-out lpf-out-2ch))))

;; ======================================================================
;; midi control
(midi-poly-player sawbble-synth)
(synth-controller sawbble-synth)
;; (midi-player-stop)

;; ======================================================================
;; some things to play with...
;; (def sawbble (sawbble-synth))
;; (stop)
;; (ctl sawbble :note 35 :gate 1)
;; (ctl sawbble :note 32)
;; (ctl sawbble :note 30)
;; (ctl sawbble :lfo-level 1.5 :lfo-freq 2.0)
;; (ctl sawbble :note-slew 25.0)
;; (ctl sawbble :lpf-freq-lo 200.0 :lpf-freq-hi 4000.0 :lpf-lfo-freq 1.5 :lpf-res 0.9)
;; (ctl sawbble :lp-res 0.75)
;; (ctl sawbble :sep-delay 2.0)
;; (ctl sawbble :gate 0)

(defn play-notes [the-inst level m notes]
  (dotimes [i (count notes)]
    (let [nx (at (m i)
                 (the-inst :note (note (notes i)) :level level))]
      (at (m (+ i 0.75)) (ctl nx :gate 0)))))

(defn slew-notes [the-inst level m notes]
  (let [nx (at (m 0) (the-inst :note 0 :level 0))]
    (dotimes [i (count notes)]
      (at (m i) (ctl nx :note (note (notes i)) :level level)))
    (at (m (+ (count notes) 1)) (ctl nx :gate 0))))

(defn play-some [the-player the-inst level]
  (let [m (metronome 140)
        notes (apply vector (flatten (repeat 3 [:c2 :g2 :e3 :e2 :f3 :f2 :g3 :c2 :c2 :c1])))]
    (the-player the-inst level m notes)))

(def my-sawbble
  (partial sawbble-synth 
           :lfo-level 1.5 :lfo-freq 0.7
           :note-slew 50.0
           :lpf-freq-lo 500.0 :lpf-freq-hi 4000.0 :lpf-lfo-freq 0.5
           :lpf-res 0.2
           :sep-delay 2.5
           ))
           
;; staccatto
(play-some play-notes my-sawbble 1.0)
;; legato
(play-some slew-notes my-sawbble 1.0)

(stop)

