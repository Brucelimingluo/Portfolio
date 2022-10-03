package client_test

// You MUST NOT change these default imports.  ANY additional imports may
// break the autograder and everyone will be sad.

import (
	// Some imports use an underscore to prevent the compiler from complaining
	// about unused imports.

	_ "encoding/hex"
	"encoding/json"
	_ "errors"
	_ "strconv"
	_ "strings"
	"testing"

	// A "dot" import is used here so that the functions in the ginko and gomega
	// modules can be used without an identifier. For example, Describe() and
	// Expect() instead of ginko.Describe() and gomega.Expect().
	"github.com/google/uuid"
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"

	userlib "github.com/cs161-staff/project2-userlib"

	"github.com/cs161-staff/project2-starter-code/client"
)

func TestSetupAndExecution(t *testing.T) {
	RegisterFailHandler(Fail)
	RunSpecs(t, "Client Tests")
}

// ================================================
// Global Variables (feel free to add more!)
// ================================================
const defaultPassword = "password"
const emptyString = ""
const contentOne = "Bitcoin is Nick's favorite "
const contentTwo = "digital "
const contentThree = "cryptocurrency!"

// ================================================
// Describe(...) blocks help you organize your tests
// into functional categories. They can be nested into
// a tree-like structure.
// ================================================

var _ = Describe("Client Tests", func() {

	// A few user declarations that may be used for testing. Remember to initialize these before you
	// attempt to use them!
	var alice *client.User
	var bob *client.User
	var charles *client.User
	var doris *client.User
	var eve *client.User

	// var frank *client.User
	// var grace *client.User
	// var horace *client.User
	// var ira *client.User
	// var emptyUser *client.User

	// These declarations may be useful for multi-session testing.
	var alicePhone *client.User
	var aliceLaptop *client.User
	var aliceDesktop *client.User

	var err error

	// A bunch of filenames that may be useful.
	aliceFile := "aliceFile.txt"
	bobFile := "bobFile.txt"
	charlesFile := "charlesFile.txt"
	dorisFile := "dorisFile.txt"
	eveFile := "eveFile.txt"
	// frankFile := "frankFile.txt"
	// graceFile := "graceFile.txt"
	// horaceFile := "horaceFile.txt"
	// iraFile := "iraFile.txt"

	BeforeEach(func() {
		// This runs before each test within this Describe block (including nested tests).
		// Here, we reset the state of Datastore and Keystore so that tests do not interfere with each other.
		// We also initialize
		userlib.DatastoreClear()
		userlib.KeystoreClear()
	})

	Describe("Basic Tests", func() {

		Specify("Basic Test: Testing InitUser/GetUser on a single user.", func() {
			userlib.DebugMsg("Initializing user Alice.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Getting user Alice.")
			aliceLaptop, err = client.GetUser("alice", defaultPassword)
			Expect(err).To(BeNil())
		})

		Specify("Basic Test: Testing Single User Store/Load/Append.", func() {
			userlib.DebugMsg("Initializing user Alice.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Storing file data: %s", contentOne)
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Appending file data: %s", contentTwo)
			err = alice.AppendToFile(aliceFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Appending file data: %s", contentThree)
			err = alice.AppendToFile(aliceFile, []byte(contentThree))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Loading file...")
			data, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo + contentThree)))
		})

		Specify("Basic Test: Testing Create/Accept Invite Functionality with multiple users and multiple instances.", func() {
			userlib.DebugMsg("Initializing users Alice (aliceDesktop) and Bob.")
			aliceDesktop, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Getting second instance of Alice - aliceLaptop")
			aliceLaptop, err = client.GetUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			// desktop store
			userlib.DebugMsg("aliceDesktop storing file %s with content: %s", aliceFile, contentOne)
			err = aliceDesktop.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())
			// laptop create invite
			userlib.DebugMsg("aliceLaptop creating invite for Bob.")
			invite, err := aliceLaptop.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())
			// bob accept
			userlib.DebugMsg("Bob accepting invite from Alice under filename %s.", bobFile)
			err = bob.AcceptInvitation("alice", invite, bobFile)
			Expect(err).To(BeNil())

			// bob append
			userlib.DebugMsg("Bob appending to file %s, content: %s", bobFile, contentTwo)
			err = bob.AppendToFile(bobFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			// aliceDesktop append
			userlib.DebugMsg("aliceDesktop appending to file %s, content: %s", aliceFile, contentThree)
			err = aliceDesktop.AppendToFile(aliceFile, []byte(contentThree))
			Expect(err).To(BeNil())

			// aliceDesktop load
			userlib.DebugMsg("Checking that aliceDesktop sees expected file data.")
			data, err := aliceDesktop.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo + contentThree)))

			// aliceLatop load
			userlib.DebugMsg("Checking that aliceLaptop sees expected file data.")
			data, err = aliceLaptop.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo + contentThree)))

			// bob load
			userlib.DebugMsg("Checking that Bob sees expected file data.")
			data, err = bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo + contentThree)))

			userlib.DebugMsg("Getting third instance of Alice - alicePhone.")
			alicePhone, err = client.GetUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Checking that alicePhone sees Alice's changes.")
			data, err = alicePhone.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo + contentThree)))
		})

		Specify("Basic Test: Testing Revoke Functionality", func() {
			userlib.DebugMsg("Initializing users Alice, Bob, and Charlie.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			charles, err = client.InitUser("charles", defaultPassword)
			Expect(err).To(BeNil())

			// alice store file
			userlib.DebugMsg("Alice storing file %s with content: %s", aliceFile, contentOne)
			alice.StoreFile(aliceFile, []byte(contentOne))

			userlib.DebugMsg("Alice creating invite for Bob for file %s, and Bob accepting invite under name %s.", aliceFile, bobFile)
			// alice invite bob
			invite, err := alice.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())

			// bob accept
			err = bob.AcceptInvitation("alice", invite, bobFile)
			Expect(err).To(BeNil())

			// alice load and check
			userlib.DebugMsg("Checking that Alice can still load the file.")
			data, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob load and check
			userlib.DebugMsg("Checking that Bob can load the file.")
			data, err = bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob invite charles
			userlib.DebugMsg("Bob creating invite for Charles for file %s, and Charlie accepting invite under name %s.", bobFile, charlesFile)
			invite, err = bob.CreateInvitation(bobFile, "charles")
			Expect(err).To(BeNil())

			// charles accept
			err = charles.AcceptInvitation("bob", invite, charlesFile)
			Expect(err).To(BeNil())

			// charles load
			userlib.DebugMsg("Checking that Charles can load the file.")
			data, err = charles.LoadFile(charlesFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// alice revoke bob
			userlib.DebugMsg("Alice revoking Bob's access from %s.", aliceFile)
			err = alice.RevokeAccess(aliceFile, "bob")
			Expect(err).To(BeNil())

			// alice load, should success
			userlib.DebugMsg("Checking that Alice can still load the file.")
			data, err = alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob load, should fail
			userlib.DebugMsg("Checking that Bob/Charles lost access to the file.")
			_, err = bob.LoadFile(bobFile)
			Expect(err).ToNot(BeNil())

			// charles load, should fail
			_, err = charles.LoadFile(charlesFile)
			Expect(err).ToNot(BeNil())

			// bob and charles cannot append
			userlib.DebugMsg("Checking that the revoked users cannot append to the file.")
			err = bob.AppendToFile(bobFile, []byte(contentTwo))
			Expect(err).ToNot(BeNil())

			err = charles.AppendToFile(charlesFile, []byte(contentTwo))
			Expect(err).ToNot(BeNil())
		})

		Specify("My tests: empty username", func() {
			userlib.DebugMsg("Initialize empty username")
			_, err = client.InitUser("", defaultPassword)
			Expect(err).ToNot(BeNil())
		})

		Specify("My tests: file storage and load basic", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			aliceFileDat, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(aliceFileDat).To(Equal([]byte(contentOne)))
		})

		Specify("My tests: single user updating file", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			err = alice.StoreFile(aliceFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			aliceFileDat, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(aliceFileDat).To(Equal([]byte(contentTwo)))
		})

		Specify("My tests: empty file content", func() {
			userlib.DebugMsg("Initializing user Alice.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())
			userlib.DebugMsg("Storing empty file")
			err = alice.StoreFile(aliceFile, []byte(""))
			Expect(err).To(BeNil())
			aliceFileDat, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(aliceFileDat).To(Equal([]byte("")))
		})

		Specify("My tests: append and update", func() {
			userlib.DebugMsg("Initializing user Alice.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			// store
			userlib.DebugMsg("Storing file data: %s", contentOne)
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())
			// append content 1
			userlib.DebugMsg("Append")
			err = alice.AppendToFile(aliceFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			// load and compare
			data, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo)))

			// update
			userlib.DebugMsg("Update file data")
			err = alice.StoreFile(aliceFile, []byte(contentThree))
			Expect(err).To(BeNil())

			// load again
			aliceFileDat, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(aliceFileDat).To(Equal([]byte(contentThree)))
		})

		Specify("My tests: two users can have same file name", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			err = alice.StoreFile(aliceFile, []byte(contentOne))

			bob, err = client.InitUser("bob", defaultPassword)
			err = bob.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())
		})

		Specify("My tests: Invitation file does not exist", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			bob, err = client.InitUser("bob", defaultPassword)
			_, err = alice.CreateInvitation("noSuchFile", "bob")
			Expect(err).ToNot(BeNil())
		})

		Specify("My tests: Wrong password", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())
			_, err = client.GetUser("alice", "wrongPassword")
			Expect(err).ToNot(BeNil())
		})

		Specify("My tests: share file does not exist", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())
			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())
			_, err = alice.CreateInvitation("noSuchFile", "yo")
			Expect(err).ToNot(BeNil())
		})

		Specify("My tests: recipient does not exist", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			// alice store file
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())
			// create invitation to Bob, who does not exist
			_, err = alice.CreateInvitation(aliceFile, "bob")
			Expect(err).ToNot(BeNil())
		})

		Specify("My tests: Single user multiple session comprehensive", func() {
			userlib.DebugMsg("Initializing users Alice (aliceDesktop) and Bob.")
			aliceDesktop, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Getting second instance of Alice - aliceLaptop")
			aliceLaptop, err = client.GetUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			// desktop store
			userlib.DebugMsg("aliceDesktop storing file %s with content: %s", aliceFile, contentOne)
			err = aliceDesktop.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			// alice laptop load and append
			data, err := aliceLaptop.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))
			userlib.DebugMsg("Appending file data: %s", contentTwo)
			err = aliceLaptop.AppendToFile(aliceFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			// alice desktop load
			data, err = aliceDesktop.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo)))

		})

		Specify("test if the invite string can be encrypted", func() {
			pkEnc, prDec, err := userlib.PKEKeyGen()
			Expect(err).To(BeNil())
			aMap := make(map[string]userlib.UUID)
			aMap["aliceeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"] = uuid.New()
			// exncrypt
			abyte, err := json.Marshal(aMap)
			Expect(err).To(BeNil())
			enc, err := userlib.PKEEnc(pkEnc, abyte)
			Expect(err).To(BeNil())

			// decrypt
			decByte, err := userlib.PKEDec(prDec, enc)
			Expect(err).To(BeNil())

			var holder map[string]userlib.UUID
			err = json.Unmarshal(decByte, &holder)
			Expect(err).To(BeNil())

		})

		Specify("My test: shared user and owner can load file", func() {

			// init alice
			userlib.DebugMsg("Initializing user Alice.")
			alice, err = client.InitUser("alice", defaultPassword)

			// init bob
			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			// alice store file
			userlib.DebugMsg("Storing file data: %s", contentOne)
			err = alice.StoreFile(aliceFile, []byte(contentOne))

			// alice share to bob
			userlib.DebugMsg("alice creating invite for Bob.")
			invite, err := alice.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())

			// bob accept
			userlib.DebugMsg("Bob accepting invite from Alice under filename %s.", bobFile)
			err = bob.AcceptInvitation("alice", invite, aliceFile)
			Expect(err).To(BeNil())

			// alice load
			data, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob load
			data_bob, err := bob.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data_bob).To(Equal([]byte(contentOne)))
		})

		Specify("My test: Not a shared user cannot load file", func() {
			// init alice
			userlib.DebugMsg("Initializing user Alice.")
			alice, err = client.InitUser("alice", defaultPassword)

			// init bob
			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			// alice store file
			userlib.DebugMsg("Storing file data: %s", contentOne)
			err = alice.StoreFile(aliceFile, []byte(contentOne))

			// alice load
			data, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob load, expected to fail
			_, err = bob.LoadFile(aliceFile)
			Expect(err).ToNot(BeNil())
		})

		Specify("My test: share and revoke, remainning user can still access, append, and reshare", func() {
			userlib.DebugMsg("Initializing users Alice, Bob, and Charlie.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			charles, err = client.InitUser("charles", defaultPassword)
			Expect(err).To(BeNil())

			doris, err = client.InitUser("doris", defaultPassword)
			Expect(err).To(BeNil())

			// alice store file
			userlib.DebugMsg("Alice storing file %s with content: %s", aliceFile, contentOne)
			alice.StoreFile(aliceFile, []byte(contentOne))

			userlib.DebugMsg("Alice creating invite for Bob for file %s, and Bob accepting invite under name %s.", aliceFile, bobFile)
			// alice invite bob
			invite, err := alice.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())

			// bob accept
			err = bob.AcceptInvitation("alice", invite, bobFile)
			Expect(err).To(BeNil())

			// alice invite doris
			invite, err = alice.CreateInvitation(aliceFile, "doris")
			Expect(err).To(BeNil())

			// doris accept
			err = doris.AcceptInvitation("alice", invite, dorisFile)
			Expect(err).To(BeNil())

			// alice load and check
			userlib.DebugMsg("Checking that Alice can still load the file.")
			data, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob load and check
			userlib.DebugMsg("Checking that Bob can load the file.")
			data, err = bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob invite charles
			userlib.DebugMsg("Bob creating invite for Charles for file %s, and Charlie accepting invite under name %s.", bobFile, charlesFile)
			invite, err = bob.CreateInvitation(bobFile, "charles")
			Expect(err).To(BeNil())

			// charles accept
			err = charles.AcceptInvitation("bob", invite, charlesFile)
			Expect(err).To(BeNil())

			// charles load
			userlib.DebugMsg("Checking that Charles can load the file.")
			data, err = charles.LoadFile(charlesFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// alice revoke bob -- only alice and dori should have access
			userlib.DebugMsg("Alice revoking Bob's access from %s.", aliceFile)
			err = alice.RevokeAccess(aliceFile, "bob")
			Expect(err).To(BeNil())

			// alice load, should success
			userlib.DebugMsg("Checking that Alice can still load the file.")
			data, err = alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob load, should fail
			userlib.DebugMsg("Checking that Bob/Charles lost access to the file.")
			_, err = bob.LoadFile(bobFile)
			Expect(err).ToNot(BeNil())

			// charles load, should fail
			_, err = charles.LoadFile(charlesFile)
			Expect(err).ToNot(BeNil())

			// bob and charles cannot append
			userlib.DebugMsg("Checking that the revoked users cannot append to the file.")
			err = bob.AppendToFile(bobFile, []byte(contentTwo))
			Expect(err).ToNot(BeNil())

			err = charles.AppendToFile(charlesFile, []byte(contentTwo))
			Expect(err).ToNot(BeNil())

			// doris can still load
			_, err = doris.LoadFile(dorisFile)
			Expect(err).To(BeNil())

			// doris can still append
			err = doris.AppendToFile(dorisFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			data, err = doris.LoadFile(dorisFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo)))

			// doris can reshare to bob
			invite, err = doris.CreateInvitation(dorisFile, "bob")
			Expect(err).To(BeNil())

			// bob accept
			err = bob.AcceptInvitation("doris", invite, bobFile)
			Expect(err).To(BeNil())

			// bob append
			err = bob.AppendToFile(bobFile, []byte(contentThree))
			Expect(err).To(BeNil())

			// bob load
			data, err = bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo + contentThree)))

		})

		Specify("My test: shared user can update", func() {
			userlib.DebugMsg("Initializing users Alice, Bob, and Charlie.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			// alice store file
			userlib.DebugMsg("Alice storing file %s with content: %s", aliceFile, contentOne)
			alice.StoreFile(aliceFile, []byte(contentOne))

			userlib.DebugMsg("Alice creating invite for Bob for file %s, and Bob accepting invite under name %s.", aliceFile, bobFile)
			// alice invite bob
			invite, err := alice.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())

			// bob accept
			err = bob.AcceptInvitation("alice", invite, bobFile)
			Expect(err).To(BeNil())

			// bob load
			data, err := bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob update
			err = bob.StoreFile(bobFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			// bob load
			data, err = bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentTwo)))

		})

		Specify("My test: revoked user cannot create invite, load, update, or append", func() {
			userlib.DebugMsg("Initializing users Alice, Bob, and Charlie.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			charles, err = client.InitUser("charles", defaultPassword)
			Expect(err).To(BeNil())

			doris, err = client.InitUser("doris", defaultPassword)
			Expect(err).To(BeNil())

			// alice store file
			userlib.DebugMsg("Alice storing file %s with content: %s", aliceFile, contentOne)
			alice.StoreFile(aliceFile, []byte(contentOne))

			userlib.DebugMsg("Alice creating invite for Bob for file %s, and Bob accepting invite under name %s.", aliceFile, bobFile)
			// alice invite bob
			invite, err := alice.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())

			// bob accept
			err = bob.AcceptInvitation("alice", invite, bobFile)
			Expect(err).To(BeNil())

			// alice invite doris
			invite, err = alice.CreateInvitation(aliceFile, "doris")
			Expect(err).To(BeNil())

			// doris accept
			err = doris.AcceptInvitation("alice", invite, dorisFile)
			Expect(err).To(BeNil())

			// alice load and check
			userlib.DebugMsg("Checking that Alice can still load the file.")
			data, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob load and check
			userlib.DebugMsg("Checking that Bob can load the file.")
			data, err = bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob invite charles
			userlib.DebugMsg("Bob creating invite for Charles for file %s, and Charlie accepting invite under name %s.", bobFile, charlesFile)
			invite, err = bob.CreateInvitation(bobFile, "charles")
			Expect(err).To(BeNil())

			// charles accept
			err = charles.AcceptInvitation("bob", invite, charlesFile)
			Expect(err).To(BeNil())

			// charles load
			userlib.DebugMsg("Checking that Charles can load the file.")
			data, err = charles.LoadFile(charlesFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// alice revoke bob -- only alice and dori should have access after this
			userlib.DebugMsg("Alice revoking Bob's access from %s.", aliceFile)
			err = alice.RevokeAccess(aliceFile, "bob")
			Expect(err).To(BeNil())

			// alice load, should success
			userlib.DebugMsg("Checking that Alice can still load the file.")
			data, err = alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob load, should fail
			userlib.DebugMsg("Checking that Bob/Charles lost access to the file.")
			_, err = bob.LoadFile(bobFile)
			Expect(err).ToNot(BeNil())

			// charles load, should fail
			_, err = charles.LoadFile(charlesFile)
			Expect(err).ToNot(BeNil())

			// bob and charles cannot append
			userlib.DebugMsg("Checking that the revoked users cannot append to the file.")
			err = bob.AppendToFile(bobFile, []byte(contentTwo))
			Expect(err).ToNot(BeNil())

			err = charles.AppendToFile(charlesFile, []byte(contentTwo))
			Expect(err).ToNot(BeNil())

			// doris can still load
			_, err = doris.LoadFile(dorisFile)
			Expect(err).To(BeNil())

			// doris can still append
			err = doris.AppendToFile(dorisFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			// doris can still load
			data, err = doris.LoadFile(dorisFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo)))

			/* -------create invite ------ */
			// doris can create invite
			eve, err = client.InitUser("eve", defaultPassword)
			Expect(err).To(BeNil())

			invite, err = doris.CreateInvitation(dorisFile, "eve")
			Expect(err).To(BeNil())

			err = eve.AcceptInvitation("doris", invite, eveFile)
			Expect(err).To(BeNil())

			// bob cannot create invite
			_, err = bob.CreateInvitation(dorisFile, "eve")
			Expect(err).ToNot(BeNil())
		})

		Specify("My test: cannot create invite on a file that's not exist/ other user's file", func() {
			userlib.DebugMsg("Initializing users Alice, Bob, and Charlie.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			// alice store file
			userlib.DebugMsg("Alice storing file %s with content: %s", aliceFile, contentOne)
			alice.StoreFile(aliceFile, []byte(contentOne))

			// alice create invalid invite -- false filename
			_, err = alice.CreateInvitation("falseFileName", "bob")
			Expect(err).To(Not(BeNil()))

			// bob create invalid invite -- other user's file
			_, err = bob.CreateInvitation(aliceFile, "alice")
			Expect(err).To(Not(BeNil()))
		})

		Specify("My test: sample", func() {
			// init alice
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			// init bob
			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			// alice store
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			// not a share user append
			err = bob.AppendToFile(aliceFile, []byte(contentTwo))
			Expect(err).ToNot(BeNil())

			_, err = bob.LoadFile(aliceFile)
			Expect(err).ToNot(BeNil())

			_, err = bob.CreateInvitation(aliceFile, "alice")
			Expect(err).ToNot(BeNil())

			// but Bob can store a file with the same name
			err = bob.StoreFile(aliceFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			// Now he can load
			bobDat, err := bob.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(bobDat).To(Equal([]byte(contentTwo)))

			// ensure it's different from alice's file
			aliceDat, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(aliceDat).To(Equal([]byte(contentOne)))

			// alice can share to bob, but bob can't accept with the same name since name collision
			invite, err := alice.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())

			err = bob.AcceptInvitation("alice", invite, aliceFile)
			Expect(err).ToNot(BeNil())

			// he needs to change the name
			err = bob.AcceptInvitation("alice", invite, bobFile)
			Expect(err).To(BeNil())

			// he can append
			err = bob.AppendToFile(bobFile, []byte(contentThree))
			Expect(err).To(BeNil())

			bobDat, err = bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(bobDat).To(Equal([]byte(contentOne + contentThree)))

			// he can update
			err = bob.StoreFile(bobFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			// check new data
			bobDat, err = bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(bobDat).To(Equal([]byte(contentTwo)))

			// alice append to the file
			err = alice.AppendToFile(aliceFile, []byte(contentThree))
			Expect(err).To(BeNil())

			// bob check
			bobDat, err = bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(bobDat).To(Equal([]byte(contentTwo + contentThree)))
		})

		Specify("My test: 3.1.1.b Bob/bob", func() {

			// init alice
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			_, err = client.InitUser("Alice", defaultPassword)
			Expect(err).To(BeNil())
		})

		Specify("My test: different user have same password", func() {

			// init alice
			alice, err = client.InitUser("alice", "123")
			Expect(err).To(BeNil())

			_, err = client.InitUser("Alice", "123")
			Expect(err).To(BeNil())
		})

		Specify("My test: revoke before accept", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			// alice store
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			// alice invite bob
			_, err = alice.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())

			// alice revoke
			err = alice.RevokeAccess(aliceFile, "bob")
			Expect(err).To(BeNil())

			// bob can't access, load, or perform any actions
			_, err = bob.LoadFile(aliceFile)
			Expect(err).ToNot(BeNil())

			err = bob.AppendToFile(aliceFile, []byte(contentOne))
			Expect(err).NotTo(BeNil())

			_, err = bob.CreateInvitation(aliceFile, "alice")
			Expect(err).NotTo(BeNil())

		})

		// start security test.
		Specify("Security test: tamper user struct mac", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			dsMap := userlib.DatastoreGetMap()

			for k, v := range dsMap {
				if len(v) == 64 {
					// then this is HMAC tmaper it
					dsMap[k] = []byte("whatever")
				}
			}

			// get User expect to fail
			alice, err = client.GetUser("alice", defaultPassword)
			Expect(err).ToNot(BeNil())

		})

		Specify("Security test: tamper user struct", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			dsMap := userlib.DatastoreGetMap()

			for k, v := range dsMap {
				if len(v) != 64 {
					// then this is HMAC tmaper it
					dsMap[k] = []byte("whatever")
				}
			}

			// get User expect to fail
			alice, err = client.GetUser("alice", defaultPassword)
			Expect(err).ToNot(BeNil())

		})

		Specify("Security test: tamper file struct", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			dsMap_pre := userlib.DatastoreGetMap()

			pre_keys := getKeys(dsMap_pre)

			// alice store file
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			dsMap_post := userlib.DatastoreGetMap()

			post_keys := getKeys(dsMap_post)

			newKeys := newKeys(pre_keys, post_keys)

			for _, v := range newKeys {
				// tamper struct
				if len(dsMap_post[v]) != 64 {
					// then this is not HMAC,tamper it
					dsMap_post[v] = []byte("whatever")
				}
			}

			// load file needs to fail
			_, err = alice.LoadFile(aliceFile)
			Expect(err).ToNot(BeNil())

		})

		Specify("Security test: tamper file struct HMAC", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			dsMap_pre := userlib.DatastoreGetMap()

			pre_keys := getKeys(dsMap_pre)

			// alice store file
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			dsMap_post := userlib.DatastoreGetMap()

			post_keys := getKeys(dsMap_post)

			newKeys := newKeys(pre_keys, post_keys)

			for _, v := range newKeys {
				// tamper struct
				if len(dsMap_post[v]) == 64 {
					// then this is HMAC,tamper it
					dsMap_post[v] = []byte("whatever")
				}
			}

			// load file needs to fail
			_, err = alice.LoadFile(aliceFile)
			Expect(err).ToNot(BeNil())

		})

		Specify("Security test: tamper all file in appended file", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			// store
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			// append
			err = alice.AppendToFile(aliceFile, []byte(contentTwo))
			Expect(err).To(BeNil())
			err = alice.AppendToFile(aliceFile, []byte(contentThree))
			Expect(err).To(BeNil())

			// tamper
			dsMap := userlib.DatastoreGetMap()

			for k, _ := range dsMap {
				// tamper any HMAC, the system should not work -- actually, tamper user struct HMAC
				// doesn't really matter with loading files, so this test could fail.
				if len(dsMap[k]) == 64 {
					// tamper the HMAC
					dsMap[k] = []byte("whatever")

					_, err = alice.LoadFile(aliceFile)
					Expect(err).ToNot(BeNil())
				}
			}

		})

		Specify("Security test: tamper after invite -- invite HMAC", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			// store
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			// append
			err = alice.AppendToFile(aliceFile, []byte(contentTwo))
			Expect(err).To(BeNil())
			err = alice.AppendToFile(aliceFile, []byte(contentThree))
			Expect(err).To(BeNil())

			// --------- take a snapthost here
			preMap := userlib.DatastoreGetMap()
			preKeys := getKeys(preMap)

			// invite
			invite, err := alice.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())

			// ------------ take another snapshot here, so that we can tamper invite struct
			postMap := userlib.DatastoreGetMap()
			postKeys := getKeys(postMap)

			newKeys := newKeys(preKeys, postKeys)

			// tamper invite Struct HMAC, at this point bob can't accept
			for _, uid := range newKeys {
				if len(uid) == 64 {
					postMap[uid] = []byte("whatever")

					// bob can't accept
					// bob accept
					err = bob.AcceptInvitation("alice", invite, bobFile)
					Expect(err).ToNot(BeNil())
				}
			}

		})

		Specify("Security test: tamper after invite -- invite struct", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			// store
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			// append
			err = alice.AppendToFile(aliceFile, []byte(contentTwo))
			Expect(err).To(BeNil())
			err = alice.AppendToFile(aliceFile, []byte(contentThree))
			Expect(err).To(BeNil())

			// --------- take a snapthost here
			preMap := userlib.DatastoreGetMap()
			preKeys := getKeys(preMap)

			// invite
			invite, err := alice.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())

			// ------------ take another snapshot here, so that we can tamper invite struct
			postMap := userlib.DatastoreGetMap()
			postKeys := getKeys(postMap)

			newKeys := newKeys(preKeys, postKeys)

			// tamper invite Struct, at this point bob can't accept
			for _, uid := range newKeys {
				if len(uid) != 64 {
					postMap[uid] = []byte("whatever")

					// bob can't accept
					// bob accept
					err = bob.AcceptInvitation("alice", invite, bobFile)
					Expect(err).ToNot(BeNil())
				}
			}

		})

		Specify("Security test: append tamp user", func() {
			var append_position1 uuid.UUID
			var append_position2 uuid.UUID
			dsMap := userlib.DatastoreGetMap()
			copy_map := make(map[string]int)

			userlib.DebugMsg("Initializing user Alice.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Storing file data: %s", contentOne)
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Appending file data: %s", contentTwo)
			err = alice.AppendToFile(aliceFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			for k, _ := range dsMap {
				if _, ok := copy_map["k"]; ok {
					continue
				} else {
					append_position1 = k
				}
			}

			userlib.DebugMsg("Appending file data: %s", contentThree)
			err = alice.AppendToFile(aliceFile, []byte(contentThree))
			Expect(err).To(BeNil())

			for k, _ := range dsMap {
				if _, ok := copy_map["k"]; ok {
					continue
				} else {
					append_position2 = k
				}
			}

			dsMap[append_position1], dsMap[append_position2] = dsMap[append_position2], dsMap[append_position1]

			userlib.DebugMsg("Loading file...")
			_, err := alice.LoadFile(aliceFile)
			Expect(err).ToNot(BeNil())

		})

		Specify("Security test: tamper everything", func() {
			userlib.DebugMsg("Initializing users Alice, Bob, and Charlie.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			charles, err = client.InitUser("charles", defaultPassword)
			Expect(err).To(BeNil())

			doris, err = client.InitUser("doris", defaultPassword)
			Expect(err).To(BeNil())

			// alice store file
			userlib.DebugMsg("Alice storing file %s with content: %s", aliceFile, contentOne)
			alice.StoreFile(aliceFile, []byte(contentOne))

			userlib.DebugMsg("Alice creating invite for Bob for file %s, and Bob accepting invite under name %s.", aliceFile, bobFile)
			// alice invite bob
			invite, err := alice.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())

			// bob accept
			err = bob.AcceptInvitation("alice", invite, bobFile)
			Expect(err).To(BeNil())

			// alice invite doris
			invite, err = alice.CreateInvitation(aliceFile, "doris")
			Expect(err).To(BeNil())

			// doris accept
			err = doris.AcceptInvitation("alice", invite, dorisFile)
			Expect(err).To(BeNil())

			// alice load and check
			userlib.DebugMsg("Checking that Alice can still load the file.")
			data, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob load and check
			userlib.DebugMsg("Checking that Bob can load the file.")
			data, err = bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob invite charles
			userlib.DebugMsg("Bob creating invite for Charles for file %s, and Charlie accepting invite under name %s.", bobFile, charlesFile)
			invite, err = bob.CreateInvitation(bobFile, "charles")
			Expect(err).To(BeNil())

			// charles accept
			err = charles.AcceptInvitation("bob", invite, charlesFile)
			Expect(err).To(BeNil())

			// charles load
			userlib.DebugMsg("Checking that Charles can load the file.")
			data, err = charles.LoadFile(charlesFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// alice revoke bob -- only alice and dori should have access after this
			userlib.DebugMsg("Alice revoking Bob's access from %s.", aliceFile)
			err = alice.RevokeAccess(aliceFile, "bob")
			Expect(err).To(BeNil())

			// alice load, should success
			userlib.DebugMsg("Checking that Alice can still load the file.")
			data, err = alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			// bob load, should fail
			userlib.DebugMsg("Checking that Bob/Charles lost access to the file.")
			_, err = bob.LoadFile(bobFile)
			Expect(err).ToNot(BeNil())

			// charles load, should fail
			_, err = charles.LoadFile(charlesFile)
			Expect(err).ToNot(BeNil())

			// bob and charles cannot append
			userlib.DebugMsg("Checking that the revoked users cannot append to the file.")
			err = bob.AppendToFile(bobFile, []byte(contentTwo))
			Expect(err).ToNot(BeNil())

			err = charles.AppendToFile(charlesFile, []byte(contentTwo))
			Expect(err).ToNot(BeNil())

			// doris can still load
			_, err = doris.LoadFile(dorisFile)
			Expect(err).To(BeNil())

			// doris can still append
			err = doris.AppendToFile(dorisFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			// doris can still load
			data, err = doris.LoadFile(dorisFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo)))

			/* -------create invite ------ */
			// doris can create invite
			eve, err = client.InitUser("eve", defaultPassword)
			Expect(err).To(BeNil())

			invite, err = doris.CreateInvitation(dorisFile, "eve")
			Expect(err).To(BeNil())

			err = eve.AcceptInvitation("doris", invite, eveFile)
			Expect(err).To(BeNil())

			// bob cannot create invite
			_, err = bob.CreateInvitation(dorisFile, "eve")
			Expect(err).ToNot(BeNil())

			/* ========================  tamper */

			dsMap := userlib.DatastoreGetMap()

			for k, _ := range dsMap {
				dsMap[k] = []byte("whatever")
			}

			alice.LoadFile(aliceFile)
			bob.LoadFile(bobFile)
			doris.LoadFile(dorisFile)
			bob.CreateInvitation(bobFile, "doris")
			doris.CreateInvitation(dorisFile, "bob")
			bob.AppendToFile(bobFile, []byte(contentTwo))
			doris.AppendToFile(dorisFile, []byte(contentTwo))
			charles.AppendToFile(charlesFile, []byte(contentThree))
			alice.StoreFile(aliceFile, []byte(contentThree))
			alice.CreateInvitation(aliceFile, "bob")

		})

		Specify("Security test: ensure inviter is inviter", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			_, err = charles.CreateInvitation(aliceFile, "bob")
			Expect(err).ToNot(BeNil())
		})

		Specify("My tests: sender does not exist", func() {
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			charles, err = client.InitUser("charles", defaultPassword)
			Expect(err).To(BeNil())

			// alice store file
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())
			// create invitation to Bob, sender does not exist
			_, err = charles.CreateInvitation(aliceFile, "bob")
			Expect(err).ToNot(BeNil())

			_, err = charles.CreateInvitation(aliceFile, "bob")
			Expect(err).ToNot(BeNil())

			_, err = doris.CreateInvitation(aliceFile, "bob")
			Expect(err).ToNot(BeNil())

		})

		Specify("My tests: swap invite", func() {

			// var data1 uuid.UUID
			// var data2 uuid.UUID
			dsMap := userlib.DatastoreGetMap()

			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			charles, err = client.InitUser("charles", defaultPassword)
			Expect(err).To(BeNil())

			doris, err = client.InitUser("doris", defaultPassword)
			Expect(err).To(BeNil())

			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())
			err = charles.StoreFile(charlesFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			data1, err := alice.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())
			data2, err := charles.CreateInvitation(charlesFile, "doris")
			Expect(err).To(BeNil())

			dsMap[data1], dsMap[data2] = dsMap[data2], dsMap[data1]

			err = bob.AcceptInvitation("alice", data1, bobFile)
			Expect(err).ToNot(BeNil())
			err = doris.AcceptInvitation("charles", data2, dorisFile)
			Expect(err).ToNot(BeNil())
		})

	})
})

// a function to get keys in a map
func getKeys(aMap map[uuid.UUID][]byte) (result []uuid.UUID) {

	for k, _ := range aMap {
		result = append(result, k)
	}
	return result
}

// return an array of keys (uuid) of newly added content in the map
func newKeys(preKeys []uuid.UUID, postKeys []uuid.UUID) (result []uuid.UUID) {

	helperMap := make(map[uuid.UUID]int)
	for i := 0; i < len(preKeys); i++ {
		helperMap[preKeys[i]] = 0 // don't get what the value is, just put it in a map
	}

	// check if exist
	for i := 0; i < len(postKeys); i++ {
		uid := postKeys[i]
		if _, ok := helperMap[uid]; !ok {
			// if there's a value in postKeys doesn't exist before, add it to result
			result = append(result, uid)
		}
	}

	return result

}
