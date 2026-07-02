(ns kami.scene.contracts-test
  (:require [clojure.test :refer [deftest is testing]]
            [kami.scene.contracts :as contracts]
            [kami.scene.fixture-sync :as fixtures]
            [kami.scene.mirror-check :as mirror]))

(deftest catalog-loads
  (let [cat (contracts/catalog)
        result (contracts/validate-catalog cat)]
    (is (:ok? result))
    (is (= 25 (:entry-count result)))
    (is (some #(= {:domain :kami-terrain-scene :file "biomes.edn"} %)
              (:contracts cat)))))

(deftest all-contract-edn-loads
  (let [result (contracts/validate-all)
        loaded (contracts/load-contracts)]
    (testing "all catalog entries are readable EDN contract data"
      (is (:ok? result))
      (is (= 25 (count loaded))))
    (testing "selected domains are present"
      (is (contains? loaded {:domain :kami-character-scene :file "hair.edn"}))
      (is (contains? loaded {:domain :kami-vehicle-scene :file "ground.edn"}))
      (is (contains? loaded {:domain :kami-live :file "audio.edn"})))))

(deftest catalog-covers-authority-source-files
  (let [coverage (:source-coverage (contracts/validate-all))
        drift (contracts/validate-source-coverage
               (update (contracts/catalog) :contracts
                       (partial remove #(= {:domain :kami-live :file "audio.edn"} %))))]
    (is (:ok? coverage))
    (is (= 25 (:source-count coverage)))
    (is (= ["kami/scene/contracts/kami-live/audio.edn"]
           (:missing-from-catalog drift)))))

(deftest legacy-engine-mirror-check
  (let [result (mirror/compare-all "../kami-engine")]
    (is (:ok? result))
    (is (= 25 (:checked result)))
    (is (every? #(or (:ok? %)
                     (:problem %))
                (:results result)))))

(deftest engine-fixture-sync-plan
  (let [plan (fixtures/sync-plan "../kami-engine")
        check (fixtures/check-plan plan)]
    (is (= 25 (count plan)))
    (is (:ok? check))
    (is (every? #{:same :retired} (map :status plan)))
    (is (some #(= {:domain :kami-terrain-scene :file "biomes.edn"} (:entry %))
              plan))))

(deftest authority-manifest-covers-engine-fixtures
  (let [{:keys [ok? contract-count manifest]} (contracts/validate-authority-manifest)
        entries (:contracts manifest)
        terrain (some #(when (= {:domain :kami-terrain-scene :file "biomes.edn"}
                         (select-keys % [:domain :file]))
                         %)
                      entries)]
    (is ok?)
    (is (= 25 contract-count))
    (is (= :engine-data-mirrors-authority (:fixture-policy manifest)))
    (is (= "kami/scene/contracts/kami-terrain-scene/biomes.edn"
           (:authority-resource terrain)))
    (is (= "orgs/kotoba-lang/kami-engine/kami-terrain-scene/data/biomes.edn"
           (:engine-fixture-path terrain)))
    (is (= 64 (count (:sha256 terrain))))
    (is (pos-int? (:byte-count terrain)))))
