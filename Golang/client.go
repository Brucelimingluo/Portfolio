package client

// CS 161 Project 2

// You MUST NOT change these default imports. ANY additional imports
// may break the autograder!

import (
	"encoding/json"

	userlib "github.com/cs161-staff/project2-userlib"
	"github.com/google/uuid"

	// hex.EncodeToString(...) is useful for converting []byte to string

	// Useful for string manipulation
	"strings"

	// Useful for formatting strings (e.g. `fmt.Sprintf`).
	"fmt"

	// Useful for creating new error messages to return using errors.New("...")
	"errors"

	// Optional.
	_ "strconv"
)

// This serves two purposes: it shows you a few useful primitives,
// and suppresses warnings for imports not being used. It can be
// safely deleted!
func someUsefulThings() {

	// Creates a random UUID.
	randomUUID := uuid.New()

	// Prints the UUID as a string. %v prints the value in a default format.
	// See https://pkg.go.dev/fmt#hdr-Printing for all Golang format string flags.
	userlib.DebugMsg("Random UUID: %v", randomUUID.String())

	// Creates a UUID deterministically, from a sequence of bytes.
	hash := userlib.Hash([]byte("user-structs/alice"))
	deterministicUUID, err := uuid.FromBytes(hash[:16])
	if err != nil {
		// Normally, we would `return err` here. But, since this function doesn't return anything,
		// we can just panic to terminate execution. ALWAYS, ALWAYS, ALWAYS check for errors! Your
		// code should have hundreds of "if err != nil { return err }" statements by the end of this
		// project. You probably want to avoid using panic statements in your own code.
		panic(errors.New("An error occurred while generating a UUID: " + err.Error()))
	}
	userlib.DebugMsg("Deterministic UUID: %v", deterministicUUID.String())

	// Declares a Course struct type, creates an instance of it, and marshals it into JSON.
	type Course struct {
		name      string
		professor []byte
	}

	course := Course{"CS 161", []byte("Nicholas Weaver")}
	courseBytes, err := json.Marshal(course)
	if err != nil {
		panic(err)
	}

	userlib.DebugMsg("Struct: %v", course)
	userlib.DebugMsg("JSON Data: %v", courseBytes)

	// Generate a random private/public keypair.
	// The "_" indicates that we don't check for the error case here.
	var pk userlib.PKEEncKey
	var sk userlib.PKEDecKey
	pk, sk, _ = userlib.PKEKeyGen()
	userlib.DebugMsg("PKE Key Pair: (%v, %v)", pk, sk)

	// Here's an example of how to use HBKDF to generate a new key from an input key.
	// Tip: generate a new key everywhere you possibly can! It's easier to generate new keys on the fly
	// instead of trying to think about all of the ways a key reuse attack could be performed. It's also easier to
	// store one key and derive multiple keys from that one key, rather than
	originalKey := userlib.RandomBytes(16)
	derivedKey, err := userlib.HashKDF(originalKey, []byte("mac-key"))
	if err != nil {
		panic(err)
	}
	userlib.DebugMsg("Original Key: %v", originalKey)
	userlib.DebugMsg("Derived Key: %v", derivedKey)

	// A couple of tips on converting between string and []byte:
	// To convert from string to []byte, use []byte("some-string-here")
	// To convert from []byte to string for debugging, use fmt.Sprintf("hello world: %s", some_byte_arr).
	// To convert from []byte to string for use in a hashmap, use hex.EncodeToString(some_byte_arr).
	// When frequently converting between []byte and string, just marshal and unmarshal the data.
	//
	// Read more: https://go.dev/blog/strings

	// Here's an example of string interpolation!
	_ = fmt.Sprintf("%s_%d", "file", 1)
}

// This is the type definition for the User struct.
// A Go struct is like a Python or Java class - it can have attributes
// (e.g. like the Username attribute) and methods (e.g. like the StoreFile method below).
type User struct {
	Username string

	// You can add other attributes here if you want! But note that in order for attributes to
	// be included when this struct is serialized to/from JSON, they must be capitalized.
	// On the flipside, if you have an attribute that you want to be able to access from
	// this struct's methods, but you DON'T want that value to be included in the serialized value
	// of this struct that's stored in datastore, then you can use a "private" variable (e.g. one that
	// begins with a lowercase letter).

	// a pair of RSA key for encryption purpose for file sharing
	PublicEnc  userlib.PKEEncKey
	PrivateDec userlib.PKEDecKey

	// a pair of RSA key for digital signature of the invitation struct
	PrivateSign_fileShare userlib.DSSignKey
	PublicVer_fileShare   userlib.DSVerifyKey

	// file uuid map: map file name to an array of [uuid_fileStruct, uuid_fileMac]
	// File_uuids map[string][]userlib.UUID

	// file key map: map file name to an array of [encKey_file, macKey_file]
	// File_keys map[string][][]byte

	// store the root key for user_file map
	RootKey_map []byte
}

// This is the type definition for the File Struct
type File struct {
	// file content
	Content []byte

	// A map from username to an array of usernames (who the user shares access to). e.g. user1: [user2, user3]
	Authorized_users map[string][]string

	// list of uuid of next file. For appending purpose
	UUID_nextFile userlib.UUID

	// list of uuid of hmac of the next file
	UUID_nextFileMac userlib.UUID

	// uuid for last file struct
	UUID_last userlib.UUID

	// uuid for mac of the last file struct
	UUID_lastMac userlib.UUID
}

// This is the type definition for the Invitation Struct
type Invite struct {
	// sender name
	SenderUsername string
	// an array of file uuids: {uuid_file, uuid_hmac}
	UUIDs []userlib.UUID
	// an array of file keys: {fileKey, macKey}
	Keys [][]byte

	// the name file owner gave this file
	OwnerFileName string
}

type userFileMap struct {
	// file uuid map: map file name to an array of [uuid_fileStruct, uuid_fileMac]
	File_uuids map[string][]userlib.UUID

	// file key map: map file name to an array of [encKey_file, macKey_file]
	File_keys map[string][][]byte
}

// NOTE: The following methods have toy (insecure!) implementations.
func InitUser(username string, password string) (userdataptr *User, err error) {
	var userdata User
	userdata.Username = username

	// check if the username is valid.
	_, hasUser := userlib.KeystoreGet(username)
	if username == "" || hasUser {
		return nil, errors.New("Invalid Username or Username already exists.")
	}

	// a root to generate uuid, symmetry key and Mac key
	root := userlib.Argon2Key([]byte(password), userlib.Hash([]byte(username)), 16)
	if err != nil {
		return nil, errors.New(err.Error())
	}

	// a root key for generating user_fileMap
	userdata.RootKey_map, err = userlib.HashKDF(root, []byte("RootKey_map"))
	userdata.RootKey_map = userdata.RootKey_map[:16]

	// symmetry key to encrypt user struct. Notice this is 64 bytes
	encKey, err := userlib.HashKDF(root, []byte("encryptStruct"))
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	// mac key to ensure Struct integrity. Notice this is 64 bytes.
	macKey, err := userlib.HashKDF(root, []byte("macKey"))
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	// A pair of enc/ dec keys for  (for file sharing)
	userdata.PublicEnc, userdata.PrivateDec, err = userlib.PKEKeyGen()
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	// a pair of verification keys for digital signature
	userdata.PrivateSign_fileShare, userdata.PublicVer_fileShare, err = userlib.DSKeyGen()
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	// generate a deterministic uuid to store User Struct in DataStore
	uuid_struct, err := uuid.FromBytes(root[:16])
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	// store encrypted user Struct in DataStore

	userdata_byte, err := json.Marshal(userdata)
	if err != nil {
		fmt.Println(err.Error())
		return
	}
	userdata_enc := userlib.SymEnc(encKey[:16], userlib.RandomBytes(16), userdata_byte)
	if err != nil {
		fmt.Println(err.Error())
		return
	}
	userlib.DatastoreSet(uuid_struct, userdata_enc)

	// Generate an 64 bytes HMAC and store the HMAC in DataStore
	hmac, err := userlib.HMACEval(macKey[:16], userdata_enc)
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	// store hmac in DataStore
	uuid_mac, err := uuid.FromBytes(hmac[:16])
	if err != nil {
		fmt.Println(err.Error())
		return
	}
	userlib.DatastoreSet(uuid_mac, hmac)

	// store public keys in Keystore
	userlib.KeystoreSet(username+"EncKey", userdata.PublicEnc)
	userlib.KeystoreSet(username+"VerKey", userdata.PublicVer_fileShare)

	/* ---------- Initialize userFile Struct. ----- */
	var filemapdata userFileMap
	filemapdata.File_uuids = make(map[string][]userlib.UUID)
	filemapdata.File_keys = make(map[string][][]byte)

	// store
	userdata.saveUserFileMap(filemapdata)

	return &userdata, nil
}

func GetUser(username string, password string) (userdataptr *User, err error) {

	// check if user exists by checking entry in keystore.
	_, hasUser := userlib.KeystoreGet(username + "EncKey")
	if !hasUser {
		return nil, errors.New("User does not exist")
	}

	// reconstruct root key
	root := userlib.Argon2Key([]byte(password), userlib.Hash([]byte(username)), 16)

	// reconstruct symmetry key
	encKey, err := userlib.HashKDF(root, []byte("encryptStruct"))
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	// reconstruct mac key
	macKey, err := userlib.HashKDF(root, []byte("macKey"))
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	// error if credential is not right (can't find uuid for user struct)
	uuid_struct, err := uuid.FromBytes(root[:16])
	userdata_enc, hasStruct := userlib.DatastoreGet(uuid_struct)

	if !hasStruct {
		return nil, errors.New("Crendential's not right")
	}

	// Check Mac, error if user Struct is tampered.
	hmac, err := userlib.HMACEval(macKey[:16], userdata_enc)
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	uuid_mac, err := uuid.FromBytes(hmac[:16])
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	hmac_stored, hasHmac := userlib.DatastoreGet(uuid_mac)

	if !hasHmac || !userlib.HMACEqual(hmac, hmac_stored) {
		return nil, errors.New("File is tampered.")
	}

	// now we can decrypt the file.
	var userdata User
	err = json.Unmarshal(userlib.SymDec(encKey[:16], userdata_enc), &userdata)
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	userdataptr = &userdata
	return userdataptr, nil
}

func (userdata *User) StoreFile(filename string, content []byte) (err error) {
	// filename here should be recipient's filename
	userInviteMap, err := userdata.getMapInMailbox(filename)
	// first check if I'm the authorized user
	if userInviteMap != nil {
		// might be a shared user, try to get invite
		keys := []string{}
		for k := range *userInviteMap {
			keys = append(keys, k)
		}
		sendername := keys[0]
		fmt.Println(sendername)
		invitedata, err := getInvite((*userInviteMap)[sendername], sendername)
		// if can't get the invite, the user might has their access revoked.
		if invitedata == nil {
			return errors.New("Your access might have been revoked.")
		}

		// proceed to update the file -- this is the first node
		filedata, err := getFile(invitedata.UUIDs, invitedata.Keys)
		if err != nil {
			return err
		}

		// loop thorugh the linked list and get rid of all content
		next_uuid := filedata.UUID_nextFile
		next_uuidMac := filedata.UUID_nextFileMac
		for next_uuid != uuid.Nil {

			// get next node -- be careful how to get UUIDs
			UUIDs := []userlib.UUID{next_uuid, next_uuidMac}
			nextFileData, err := getFile(UUIDs, invitedata.Keys)
			if err != nil {
				return err
			}

			ptr := nextFileData.UUID_nextFile
			ptrMac := nextFileData.UUID_nextFileMac

			// delete record for file Sturt and hmac
			userlib.DatastoreDelete(next_uuid)
			userlib.DatastoreDelete(next_uuidMac)

			// update pointer
			next_uuid = ptr
			next_uuidMac = ptrMac
		}

		// since it's an update. Only one node is left, update first node
		filedata.Content = content
		filedata.UUID_nextFile = uuid.Nil
		filedata.UUID_nextFileMac = uuid.Nil
		filedata.UUID_last = invitedata.UUIDs[0]
		filedata.UUID_lastMac = invitedata.UUIDs[1]

		// encrypt and store the encrypted first node
		err = saveFile(invitedata.UUIDs, invitedata.Keys, *filedata)
		if err != nil {
			return err
		}

		return err
	}

	// might be the file owner trying to first time store or make update
	/* ------------------------Confirm that this is --------------------- */

	// initialize File Struct and corresponding fields
	var filedata File
	filedata.Content = content
	filedata.Authorized_users = make(map[string][]string)
	// the owner is indeed authorized, but since we don't really distinguish owner and other authorized
	//   users in the requirement. So I made it easier here.
	filedata.Authorized_users[userdata.Username] = []string{}

	// get map
	filemapdata, err := userdata.getUserFileMap()
	if err != nil {
		return err
	}
	if filemapdata == nil {
		// since every user is initialize with filemap
		fmt.Println("Are you really the owner of the file? Suspicious.")
		return
	}

	// check if the owner is first time storing

	if filemapdata.File_uuids[filename] == nil {

		// first time storing by owner

		uuid_fileStruct := uuid.New()
		uuid_fileMac := uuid.New()
		// userdata.File_uuids = make(map[string][]userlib.UUID)
		// userdata.File_uuids[filename] = []userlib.UUID{uuid_fileStruct, uuid_fileMac}

		// generate and store encKey and macKey, all 16 bytes.
		encKey_file := userlib.RandomBytes(16)
		mackey_file := userlib.RandomBytes(16)

		// userdata.File_keys = make(map[string][][]byte)
		// userdata.File_keys[filename] = [][]byte{encKey_file, mackey_file}

		// update uuid last
		filedata.UUID_last = uuid_fileStruct
		filedata.UUID_lastMac = uuid_fileMac

		/* ---------- update userFile Struct. ----- */
		filemapdata.File_uuids = make(map[string][]userlib.UUID)
		filemapdata.File_uuids[filename] = []userlib.UUID{uuid_fileStruct, uuid_fileMac}
		filemapdata.File_keys = make(map[string][][]byte)
		filemapdata.File_keys[filename] = [][]byte{encKey_file, mackey_file}

		// store
		err = userdata.saveUserFileMap(*filemapdata)
		if err != nil {
			return err
		}
	} else {
		// updating

		// get first node of the file -- if owner
		filedata, err := getFile(filemapdata.File_uuids[filename], filemapdata.File_keys[filename])
		if err != nil {
			return err
		}

		// loop thorugh the linked list and get rid of all content
		next_uuid := filedata.UUID_nextFile
		next_uuidMac := filedata.UUID_nextFileMac
		for next_uuid != uuid.Nil {

			// get next node -- be careful how to get UUIDs
			UUIDs := []userlib.UUID{next_uuid, next_uuidMac}
			nextFileData, err := getFile(UUIDs, filemapdata.File_keys[filename])
			if err != nil {
				return err
			}

			ptr := nextFileData.UUID_nextFile
			ptrMac := nextFileData.UUID_nextFileMac

			// delete record for file Sturt and hmac
			userlib.DatastoreDelete(next_uuid)
			userlib.DatastoreDelete(next_uuidMac)

			// update pointer
			next_uuid = ptr
			next_uuidMac = ptrMac
		}

		// since it's an update. Only one node is left
		filedata.UUID_nextFile = uuid.Nil
		filedata.UUID_nextFileMac = uuid.Nil
		filedata.UUID_last = filemapdata.File_uuids[filename][0]
		filedata.UUID_lastMac = filemapdata.File_uuids[filename][1]
	}

	// encrypt and store the encrypted File Struct
	filedata_byte, err := json.Marshal(filedata)
	if err != nil {
		fmt.Println(err.Error())
		return err
	}
	filedata_enc := userlib.SymEnc(filemapdata.File_keys[filename][0], userlib.RandomBytes(16), filedata_byte)
	userlib.DatastoreSet(filemapdata.File_uuids[filename][0], filedata_enc)

	// encrypt and store the File Struct HMAC
	filedata_hmac, err := userlib.HMACEval(filemapdata.File_keys[filename][1], filedata_enc)
	if err != nil {
		fmt.Println(err.Error())
		return err
	}
	userlib.DatastoreSet(filemapdata.File_uuids[filename][1], filedata_hmac)

	return
}

func (userdata *User) AppendToFile(filename string, content []byte) error {

	// get the first node, be careful how to achieve UUID
	UUIDs, KEYs, err := userdata.assignFileIDandKEY(filename)
	if err != nil {
		return err
	}

	// proceed to get file -- first node
	filedata, err := getFile(UUIDs, KEYs)
	if err != nil {
		return err
	}

	/* --------------------------------------------------------- */
	// decide a new uuid for new Append and store in File Struct
	uuid_append := uuid.New()

	// decide a new uuid for HMAC for new Append
	uuid_appendHMAC := uuid.New()

	// instantiate a new file struct for this append
	var newAppend File
	newAppend.Content = content
	newAppend.Authorized_users = nil // doesn't really matter. So leave it as nil
	newAppend.UUID_nextFile = uuid.Nil
	newAppend.UUID_nextFileMac = uuid.Nil
	newAppend.UUID_last = uuid.Nil

	// encrypt and store the new append file Struct using the same key
	saveFile([]userlib.UUID{uuid_append, uuid_appendHMAC}, KEYs, newAppend)

	/* --------------------------------------------------------------------*/

	if filedata.UUID_last == UUIDs[0] {
		// if only one node
		filedata.UUID_nextFile = uuid_append
		filedata.UUID_nextFileMac = uuid_appendHMAC
		filedata.UUID_last = uuid_append
		filedata.UUID_lastMac = uuid_appendHMAC

		// re-encrypt first struct -- ** note that it's the first node **
		saveFile(UUIDs, KEYs, *filedata)
	} else {
		// if multiple nodes

		// proceed to withdraw the last file struct, which is for us to append
		last_UUIDs := []userlib.UUID{filedata.UUID_last, filedata.UUID_lastMac}
		lastFiledata, err := getFile(last_UUIDs, KEYs)
		if err != nil {
			fmt.Println(err.Error())
			return err
		}

		// update last file fields
		lastFiledata.UUID_nextFile = uuid_append
		lastFiledata.UUID_nextFileMac = uuid_appendHMAC
		lastFiledata.UUID_last = uuid_append
		lastFiledata.UUID_lastMac = uuid_appendHMAC

		// re-encrypt and store the last struct back to DataStore -- **note that should use the un-updated first node's last id**
		saveFile([]userlib.UUID{filedata.UUID_last, filedata.UUID_lastMac}, KEYs, *lastFiledata)

		// update first strut
		filedata.UUID_last = uuid_append
		filedata.UUID_lastMac = uuid_appendHMAC

		// re-encrypt first file
		saveFile(UUIDs, KEYs, *filedata)
	}

	return nil
}

func (userdata *User) LoadFile(filename string) (content []byte, err error) {

	UUIDs, KEYs, err := userdata.assignFileIDandKEY(filename)
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	// get filemap
	filemapdata, err := userdata.getUserFileMap()
	if err != nil {
		return nil, err
	}
	if filemapdata == nil {
		fmt.Println("Are you really the owner file? Suspicious.")
		return nil, err
	}

	// get the file -- if owner (integrity and authenticity checked)
	filedata, err := getFile(UUIDs, KEYs)
	if err != nil {
		return nil, err
	}

	// decrept the whole structure
	next_uuid := filedata.UUID_nextFile
	next_uuid_mac := filedata.UUID_nextFileMac

	finalContent := filedata.Content

	for next_uuid != uuid.Nil {
		// get the file for each node: **note that every node are encrypted with the same keys, only the uuid are different**

		// also note that the way to get UUID and KEYs is different between owner and shared authorized user
		UUIDs := []userlib.UUID{next_uuid, next_uuid_mac}
		filedata, err := getFile(UUIDs, KEYs)
		if err != nil {
			return nil, err
		}

		// append
		finalContent = append(finalContent, filedata.Content...)

		// update pointer
		next_uuid = filedata.UUID_nextFile
		next_uuid_mac = filedata.UUID_nextFileMac
	}

	return finalContent, err
}

func (userdata *User) CreateInvitation(filename string, recipientUsername string) (
	invitationPtr uuid.UUID, err error) {

	// decide if "I" am owner or shared authorized user
	UUIDs, KEYs, err := userdata.assignFileIDandKEY(filename)
	if err != nil {
		return uuid.Nil, err
	}

	// check if "I" could get the file in the first place, if not, that means "I" am a revoked user
	filedata, err := getFile(UUIDs, KEYs)
	if filedata == nil {
		return uuid.Nil, errors.New("Bruh you can't even get the file yourself, what's the invite for?")
	}

	// get filemap
	filemapdata, err := userdata.getUserFileMap()
	if err != nil {
		return uuid.Nil, err
	}
	if filemapdata == nil {
		fmt.Println("Are you really the owner file? Suspicious.")
		return uuid.Nil, err
	}

	// check if recipient exists
	_, ok := userlib.KeystoreGet(recipientUsername + "EncKey")
	if !ok {
		return uuid.Nil, errors.New("Recipient does not exist!")
	}

	// proceed to create invitation struct.
	var inviteDat Invite
	inviteDat.UUIDs = UUIDs
	inviteDat.Keys = KEYs
	inviteDat.SenderUsername = userdata.Username
	inviteDat.OwnerFileName = filename

	// generate a random uuid for invite struct and generate a symmetry key which known
	//  only to the inviter and recipient
	uuid_invite := uuid.New()

	// save the invite Struct in DataStore
	err = userdata.saveInvite(uuid_invite, inviteDat)
	if err != nil {
		return uuid.Nil, err
	}

	return uuid_invite, err
}

func (userdata *User) AcceptInvitation(senderUsername string, invitationPtr uuid.UUID, filename string) error {

	// get filemap
	filemapdata, err := userdata.getUserFileMap()
	if err != nil {
		return err
	}
	if filemapdata == nil {
		fmt.Println("Are you really the owner file? Suspicious.")
		return err
	}

	// check if "I" already have this file
	if filemapdata.File_uuids[filename] != nil {
		return errors.New("Recipient already has this file.")
	}

	// get the invite struct (integrity and authenticity of the invite is checked when loading inside this function)
	inviteDat, err := getInvite(invitationPtr, senderUsername)
	if err != nil {
		return err
	}

	// Check if the file is indeed sent by the sender (actually this is checked by signature already.. but well)
	if inviteDat.SenderUsername != senderUsername {
		return errors.New("File is not sent by the sender!")
	}

	/* ------------------------------------grant "me" the access -------------------------- */

	// create a mailbox and save the map {sendername: uuid_invitation} to the mailbox
	//  -- filename here should be recipient's filename
	saveMapInMailbox(userdata.Username, filename, senderUsername, invitationPtr)

	// update this info in filenamemap

	fileNameMap := make(map[string]string)
	fileNameMap[inviteDat.OwnerFileName+userdata.Username] = filename

	err = saveFileNameMap(inviteDat.OwnerFileName, userdata.Username, fileNameMap)
	if err != nil {
		return err
	}

	/* -------------------------------------update file struct------------------------------------------ */
	// get the file and update (re-encrypt) that:
	// 1). "I" am a authorized user
	// 2).  The sender has authorized me with the access.

	// Note: should be getting the file from the invitation struct,
	//     instead of the filemap data (which should only be owned by the file owner)

	filedata, err := getFile(inviteDat.UUIDs, inviteDat.Keys)
	if err != nil {
		return err
	}

	// 1) update I'm the authorized user
	filedata.Authorized_users[userdata.Username] = []string{}
	// 2) update the sender has added me as authorized user
	sender_auth_list := filedata.Authorized_users[senderUsername]
	sender_auth_list = append(sender_auth_list, userdata.Username)
	filedata.Authorized_users[senderUsername] = sender_auth_list

	/* --- re- encrypt and store the encrypted File Struct ----- */
	err = saveFile(inviteDat.UUIDs, inviteDat.Keys, *filedata)
	if err != nil {
		return err
	}

	/* ------------------------------------------------------------------------- */

	return err
}

func (userdata *User) RevokeAccess(filename string, recipientUsername string) error {
	// generate new uuid for first file -- this is enough, since all users (even the owner) know just the first node anyway
	uuid_fileStruct := uuid.New()
	uuid_fileMac := uuid.New()

	// generate and store encKey and macKey, all 16 bytes.
	encKey_file := userlib.RandomBytes(16)
	mackey_file := userlib.RandomBytes(16)

	/* --- update "my"(the owner's) filemap struct ------------  */
	filemapdata, err := userdata.getUserFileMap()
	if err != nil {
		return err
	}

	// this is to record the old file address
	old_UUIDs := filemapdata.File_uuids[filename]
	old_KEYs := filemapdata.File_keys[filename]

	new_UUIDs := []userlib.UUID{uuid_fileStruct, uuid_fileMac}
	new_KEYs := [][]byte{encKey_file, mackey_file}

	filemapdata.File_uuids[filename] = []userlib.UUID{uuid_fileStruct, uuid_fileMac}
	filemapdata.File_keys[filename] = [][]byte{encKey_file, mackey_file}

	err = userdata.saveUserFileMap(*filemapdata)
	if err != nil {
		return err
	}

	/* ---------- update File Struct (udpdate the list of authorized users).
		1). Remove recipient's name in "my" list
		2)	For every name in recipient's list, remove their whole entry in the map too
		3). Remove the whole share list created by the recipient (the key-value pair {recipient: [share1, share2, ...]}
		4). update uuid_last, uuid_lastMac -- if the file has only one node.
		5). Move the struct and file to another location
	----- */
	filedata, err := getFile(old_UUIDs, old_KEYs)
	// 1)
	slice := filedata.Authorized_users[userdata.Username]
	newList := []string{}
	for _, v := range slice {
		if v != recipientUsername {
			newList = append(newList, v)
		}
	}
	filedata.Authorized_users[userdata.Username] = newList

	// 2)
	recipient_shared_list := filedata.Authorized_users[recipientUsername]
	for _, v := range recipient_shared_list {
		delete(filedata.Authorized_users, v) // this is in place
	}

	// 3)
	delete(filedata.Authorized_users, recipientUsername)

	// 4)
	if filedata.UUID_nextFile == uuid.Nil {
		// if only one node, need to update uuid_last
		filedata.UUID_last = uuid_fileStruct
		filedata.UUID_lastMac = uuid_fileMac
	}

	// 5)
	saveFile(new_UUIDs, new_KEYs, *filedata)

	/* --- update (create) new invite Struct and store it in a new location. ----
	1) delete old invite? -- check
	*/
	var newInvite Invite
	newInvite.UUIDs = new_UUIDs
	newInvite.Keys = new_KEYs
	newInvite.SenderUsername = userdata.Username
	newInvite.OwnerFileName = filename

	// new invite uuid
	uuid_newInvite := uuid.New()
	err = userdata.saveInvite(uuid_newInvite, newInvite)
	if err != nil {
		return err
	}

	/* --- notify all other users by update their mailbox with the new invite Struct location. */
	authorizedMap := filedata.Authorized_users
	for key, _ := range authorizedMap {
		if key != userdata.Username {
			// don't need to update the owner
			// key here is each recipient's username
			// --- need a map to map from owner's filename to recipient's filenmae
			fileNameMap, err := getFileNameMap(filename, key)
			if err != nil {
				return err
			}
			recipientFileName := (*fileNameMap)[filename+key]

			//  -- notice filename here should be recipient's filename,
			saveMapInMailbox(key, recipientFileName, userdata.Username, uuid_newInvite)
			for _, v := range authorizedMap[key] {
				// here v is eaach recipient's username
				// update all other users
				fileNameMap_new, err := getFileNameMap(filename, v)
				if err != nil {
					return err
				}
				recipientFileName_new := (*fileNameMap_new)[filename+v]
				saveMapInMailbox(v, recipientFileName_new, userdata.Username, uuid_newInvite)
			}
		}
	}

	/* ---------------delete file at old location--------------------- */
	userlib.DatastoreDelete(old_UUIDs[0])
	userlib.DatastoreDelete(old_UUIDs[1])

	return nil
}

/* -------------- Helper function I created --------------*/

// only file owner can call
func (userdata *User) saveUserFileMap(mapdata userFileMap) (err error) {

	// get keys
	symKey, err := userlib.HashKDF(userdata.RootKey_map[:16], []byte("file map symmetry key"))
	if err != nil {
		return errors.New(err.Error())
	}
	macKey, err := userlib.HashKDF(userdata.RootKey_map[:16], []byte("file map HMAC key"))
	if err != nil {
		return errors.New(err.Error())
	}

	// encrypt the file
	mapdata_byte, err := json.Marshal(mapdata)
	if err != nil {
		return errors.New(err.Error())
	}
	mapdata_enc := userlib.SymEnc(symKey[:16], userlib.RandomBytes(16), mapdata_byte)
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	// Generate an 64 bytes HMAC and store the HMAC in DataStore
	hmac, err := userlib.HMACEval(macKey[:16], mapdata_enc)
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	// store hmac in DataStore
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	// get uuid
	uuid_fileMap, err := uuid.FromBytes(userdata.RootKey_map[:16])
	if err != nil {
		return errors.New(err.Error())
	}
	uuid_mac, err := uuid.FromBytes(hmac[:16])

	// store map struct and it's mac
	userlib.DatastoreSet(uuid_fileMap, mapdata_enc)
	userlib.DatastoreSet(uuid_mac, hmac)

	return err
}

// only file owner can call
func (userdata *User) getUserFileMap() (fileMapPtr *userFileMap, err error) {
	// get keys
	symKey, err := userlib.HashKDF(userdata.RootKey_map[:16], []byte("file map symmetry key"))
	if err != nil {
		return nil, errors.New(err.Error())
	}
	macKey, err := userlib.HashKDF(userdata.RootKey_map[:16], []byte("file map HMAC key"))
	if err != nil {
		return nil, errors.New(err.Error())
	}

	// get mapFlie

	// error if credential is not right (can't find uuid for user struct)
	uuid_fileMap, err := uuid.FromBytes(userdata.RootKey_map[:16])
	if err != nil {
		fmt.Println(err.Error())
		return nil, err
	}
	mapdata_enc, hasStruct := userlib.DatastoreGet(uuid_fileMap)
	if !hasStruct {
		return nil, errors.New("No FileMap found. Are you really the owner of the file?")
	}

	// Check Mac, error if user Struct is tampered.
	hmac, err := userlib.HMACEval(macKey[:16], mapdata_enc)
	if err != nil {
		fmt.Println(err.Error())
		return nil, err
	}

	uuid_mac, err := uuid.FromBytes(hmac[:16])
	if err != nil {
		fmt.Println(err.Error())
		return nil, err
	}

	hmac_stored, hasHmac := userlib.DatastoreGet(uuid_mac)
	if !hasHmac || !userlib.HMACEqual(hmac, hmac_stored) {
		return nil, errors.New("File is tampered.")
	}

	// now we can decrypt the file.
	var mapdata userFileMap
	err = json.Unmarshal(userlib.SymDec(symKey[:16], mapdata_enc), &mapdata)
	if err != nil {
		fmt.Println(err.Error())
		return nil, err
	}

	return &mapdata, err

}

// only invite creater can call
func (userdata *User) saveInvite(uuid_invite userlib.UUID, invitedata Invite) (err error) {

	// derive a symmetry key known only to inviter and invitee
	symKey, err := userlib.HashKDF(uuid_invite[:16], []byte("invite struct symmetry key"))
	if err != nil {
		return err
	}

	// encrypt with the symmetry key
	inviteDat_byte, err := json.Marshal(invitedata)
	if err != nil {
		return err
	}
	inviteDat_byte_enc := userlib.SymEnc(symKey[:16], userlib.RandomBytes(16), inviteDat_byte)

	// generate signature and store the signature in DataStore
	inviteDat_signature, err := userlib.DSSign(userdata.PrivateSign_fileShare, inviteDat_byte_enc)
	if err != nil {
		return err
	}

	// store invite signature, uuid_signature is known only to inviter and invitee
	uuid_signature, err := uuid.FromBytes(userlib.Hash(append(uuid_invite[:], []byte("uuid for signature")...))[:16])
	if err != nil {
		return err
	}

	// store invie struct and it's signature
	userlib.DatastoreSet(uuid_invite, inviteDat_byte_enc)
	userlib.DatastoreSet(uuid_signature, inviteDat_signature)

	return err
}

// note that this is callable by anyone who KNOWS the uuid of the invite and the sender of the invite
func getInvite(uuid_invite userlib.UUID, senderUsername string) (invitePtr *Invite, err error) {
	// Verify integrity and authenticity by checking signature, using sender's public verification key.
	uuid_signature, err := uuid.FromBytes(userlib.Hash(append(uuid_invite[:], []byte("uuid for signature")...))[:16])
	if err != nil {
		return nil, err
	}

	// get encrypted file
	inviteDat_byte_enc, ok := userlib.DatastoreGet(uuid_invite)
	if !ok {
		return nil, errors.New("Can't find this file at the given location.")
	}

	// get signature
	inviteDat_signature, ok := userlib.DatastoreGet(uuid_signature)
	if !ok {
		return nil, errors.New("Can't find the corresponding digital signature.")
	}

	verKey, ok := userlib.KeystoreGet(senderUsername + "VerKey")
	if !ok {
		return nil, errors.New("No such sender.")
	}
	err = userlib.DSVerify(verKey, inviteDat_byte_enc, inviteDat_signature)
	if err != nil {
		return nil, err
	}

	// proceed to decrypt the file using the symmetry key derived from uuid

	symKey, err := userlib.HashKDF(uuid_invite[:16], []byte("invite struct symmetry key"))
	if err != nil {
		return nil, err
	}
	inviteDat_byte := userlib.SymDec(symKey[:16], inviteDat_byte_enc)
	var inviteDat Invite
	json.Unmarshal(inviteDat_byte, &inviteDat)

	return &inviteDat, err
}

// anyone can save the {sendername: invitationPtr} map in a user's mailbox for a specific file (the uuid of mailbox would not change)
// filename here should be the recipient's filename
func saveMapInMailbox(recipientName string, filename string, senderUsername string, invitationPtr userlib.UUID) (err error) {
	// create a mailbox for this file -- first generate uuids
	a := userlib.Hash([]byte(recipientName + filename))
	uuid_mailbox, err := uuid.FromBytes(a[:16])
	if err != nil {
		return err
	}
	// get the uuid of hmac from the uuid_mailbox
	b := userlib.Hash(append(uuid_mailbox[:], []byte("uuid for mailbox hmac")...))
	uuid_hmac, err := uuid.FromBytes(b[:16])
	if err != nil {
		return err
	}

	// create the map that's going to be stored in the mailbox
	inviteUuidMap := make(map[string]userlib.UUID)
	inviteUuidMap[senderUsername] = invitationPtr

	// encrypt the map using my public key (ensure the map is lightweight)
	inviteUuidMap_byte, err := json.Marshal(inviteUuidMap)
	if err != nil {
		return err
	}

	pkEncKey, ok := userlib.KeystoreGet(recipientName + "EncKey")
	if !ok {
		return errors.New("No public key found.")
	}

	inviteUuidMap_byte_sec, err := userlib.PKEEnc(pkEncKey, inviteUuidMap_byte)
	if err != nil {
		return err
	}

	// calculate a hmac
	macKey, err := userlib.HashKDF(uuid_mailbox[:], []byte("hmac key for mailbox"))
	if err != nil {
		return err
	}
	hmac, err := userlib.HMACEval(macKey[:16], inviteUuidMap_byte_sec)
	if err != nil {
		return err
	}

	// store the map and it's hmac
	userlib.DatastoreSet(uuid_mailbox, inviteUuidMap_byte_sec)
	userlib.DatastoreSet(uuid_hmac, hmac)

	return err
}

// only the owner of the mailbox can call this to get the invitation map inside
func (userdata *User) getMapInMailbox(filename string) (mapPtr *map[string]userlib.UUID, err error) {
	// create a mailbox for this file -- first generate uuids
	a := userlib.Hash([]byte(userdata.Username + filename))
	uuid_mailbox, err := uuid.FromBytes(a[:16])
	if err != nil {
		return nil, err
	}
	// get the uuid of hmac from the uuid_mailbox
	b := userlib.Hash(append(uuid_mailbox[:], []byte("uuid for mailbox hmac")...))
	uuid_hmac, err := uuid.FromBytes(b[:16])
	if err != nil {
		return nil, err
	}

	// get encrypted map from datastore and its hmac
	inviteUuidMap_byte_sec, ok := userlib.DatastoreGet(uuid_mailbox)
	if !ok {
		return nil, errors.New("Can't find the map. Have you accepted a invite of this file before?")
	}
	hmac_stored, ok := userlib.DatastoreGet(uuid_hmac)
	if !ok {
		return nil, errors.New("Can't find the map. Have you accepted a invite of this file before?")
	}

	// check mac -- first calculate a hmac
	macKey, err := userlib.HashKDF(uuid_mailbox[:], []byte("hmac key for mailbox"))
	if err != nil {
		return nil, err
	}
	hmac, err := userlib.HMACEval(macKey[:16], inviteUuidMap_byte_sec)
	if err != nil {
		return nil, err
	}

	// check mac
	equal := userlib.HMACEqual(hmac, hmac_stored)
	if !equal {
		return nil, errors.New("The mailbox has been tampered! Somebody try to mess with the shared files.")
	}

	// then we can proceed to decrypt the map
	inviteUuidMap_byte, err := userlib.PKEDec(userdata.PrivateDec, inviteUuidMap_byte_sec)
	if err != nil {
		return nil, err
	}

	var holder map[string]userlib.UUID
	err = json.Unmarshal(inviteUuidMap_byte, &holder)

	return &holder, err
}

//

// anyone who knows the uuids and keys of the file can get the file.
// --- note that this only gets you a single node of the file
// @parameter UUIDs: [uuid_file, uuid_mac]
// @parameter KEYs: [symKey, macKey]
func getFile(UUIDs []userlib.UUID, KEYs [][]byte) (filePtr *File, err error) {
	// get HMAC and encrypted File Struct
	file_hmac_stored, ok := userlib.DatastoreGet(UUIDs[1])
	if !ok {
		return nil, errors.New(strings.ToTitle("HMAC of File not found"))
	}
	filedata_enc_byte, ok := userlib.DatastoreGet(UUIDs[0])
	if !ok {
		return nil, errors.New(strings.ToTitle("file not found"))
	}

	// compute and check for HMAC
	file_hmac, err := userlib.HMACEval(KEYs[1], filedata_enc_byte)
	if err != nil {
		fmt.Println(err.Error())
		return nil, err
	}
	if !userlib.HMACEqual(file_hmac, file_hmac_stored) {
		return nil, errors.New("File is tampered!")
	}

	// proceed to decrypt file
	filedata_byte := userlib.SymDec(KEYs[0], filedata_enc_byte)

	var filedata File
	err = json.Unmarshal(filedata_byte, &filedata)
	if err != nil {
		fmt.Println(err.Error())
		return nil, err
	}

	return &filedata, err
}

// save file and it's hmac, only stored one node. Anyone can call
// @parameter UUIDs: [uuid_file, uuid_mac]
func saveFile(UUIDs []userlib.UUID, KEYs [][]byte, filedata File) (err error) {
	// encrypt and store the encrypted File Struct
	filedata_byte, err := json.Marshal(filedata)
	if err != nil {
		fmt.Println(err.Error())
		return err
	}
	filedata_enc := userlib.SymEnc(KEYs[0], userlib.RandomBytes(16), filedata_byte)
	userlib.DatastoreSet(UUIDs[0], filedata_enc)

	// encrypt and store the File Struct HMAC
	filedata_hmac, err := userlib.HMACEval(KEYs[1], filedata_enc)
	if err != nil {
		fmt.Println(err.Error())
		return err
	}
	userlib.DatastoreSet(UUIDs[1], filedata_hmac)

	return err
}

// assign UUID and KEYs to load file
func (userdata *User) assignFileIDandKEY(filename string) (UUIDs []userlib.UUID, KEYs [][]byte, err error) {

	userInviteMap, err := userdata.getMapInMailbox(filename)
	// first check if I'm the authorized user
	if userInviteMap == nil {
		// not a shared user but might be the owner

		fileMapdata, err := userdata.getUserFileMap()
		if fileMapdata == nil {
			return nil, nil, errors.New("Not authorized to perform action.")
		}
		// check if the filename is in the map, if not, it's either owner or shared user
		if fileMapdata.File_uuids[filename] == nil {
			return nil, nil, errors.New("Can't access this file. You are not an owner.")
		}

		return fileMapdata.File_uuids[filename], fileMapdata.File_keys[filename], err

	} else {
		// might be a shared user, try to get invite
		keys := []string{}
		for k := range *userInviteMap {
			keys = append(keys, k)
		}
		sendername := keys[0]
		invitedata, err := getInvite((*userInviteMap)[sendername], sendername)
		// if can't get the invite, the user might has their access revoked.
		if invitedata == nil {
			return nil, nil, errors.New("Your access might have been revoked.")
		}

		// proceed to assign

		return invitedata.UUIDs, invitedata.Keys, err

	}

}

// a map from owner's filename to recipient's filename {"ownerFileName"+"recipientUsername" : "recipientFileName"}
// this is callable by both file owner and recipient when accepting invite
func saveFileNameMap(ownerFileName string, recipientUsername string, mapdata map[string]string) (err error) {

	a := userlib.Hash([]byte(ownerFileName + recipientUsername))
	uuid_nameMap, err := uuid.FromBytes(a[:16])
	if err != nil {
		return err
	}

	// derive a symmetry key known only to fileowner -- for each file shared to a user
	symKey, err := userlib.HashKDF(uuid_nameMap[:16], []byte("owner file name map symmetry key"))
	if err != nil {
		return err
	}

	// encrypt with the symmetry key
	nameMap_byte, err := json.Marshal(mapdata)
	if err != nil {
		return err
	}
	nameMap_byte_enc := userlib.SymEnc(symKey[:16], userlib.RandomBytes(16), nameMap_byte)

	// generate a HMAC and store it
	b := userlib.Hash([]byte(ownerFileName + recipientUsername + "HMAC key"))
	uuid_nameMapMac, err := uuid.FromBytes(b[:16])
	if err != nil {
		return err
	}

	// derive hmac key
	macKey, err := userlib.HashKDF(uuid_nameMapMac[:16], []byte("HMAC Key for file name map of owner"))
	if err != nil {
		return err
	}

	// compute hmac
	hmac, err := userlib.HMACEval(macKey[:16], nameMap_byte_enc)
	if err != nil {
		return err
	}

	// store map and hmac
	userlib.DatastoreSet(uuid_nameMap, nameMap_byte_enc)
	userlib.DatastoreSet(uuid_nameMapMac, hmac)

	return err
}

// this is callable by both the file owner, and the recipient, when accepting invite
// the map is {"ownerFileName"+"recipientUsername" : "recipientFileName"}
func getFileNameMap(ownerFileName string, recipientUsername string) (mapPtr *map[string]string, err error) {

	// derive uuid and key
	a := userlib.Hash([]byte(ownerFileName + recipientUsername))
	uuid_nameMap, err := uuid.FromBytes(a[:16])
	if err != nil {
		return nil, err
	}

	symKey, err := userlib.HashKDF(uuid_nameMap[:16], []byte("owner file name map symmetry key"))
	if err != nil {
		return nil, err
	}

	// get the map from datastore
	nameMap_byte_enc, ok := userlib.DatastoreGet(uuid_nameMap)
	if !ok {
		return nil, errors.New("Did recipient accept the invite? Did the invite exist?")
	}

	// generate a HMAC and store it
	b := userlib.Hash([]byte(ownerFileName + recipientUsername + "HMAC key"))
	uuid_nameMapMac, err := uuid.FromBytes(b[:16])
	if err != nil {
		return nil, err
	}

	// derive hmac key
	macKey, err := userlib.HashKDF(uuid_nameMapMac[:16], []byte("HMAC Key for file name map of owner"))
	if err != nil {
		return nil, err
	}

	// compute hmac
	hmac, err := userlib.HMACEval(macKey[:16], nameMap_byte_enc)
	if err != nil {
		return nil, err
	}

	// check mac
	hmac_stored, ok := userlib.DatastoreGet(uuid_nameMapMac)
	if !ok {
		return nil, errors.New("Did recipient accept the invite? Did the invite exist?")
	}

	same := userlib.HMACEqual(hmac_stored, hmac)
	if !same {
		return nil, errors.New("File is tampered!")
	}

	// proceed to decrypt file
	nameMap_byte := userlib.SymDec(symKey[:16], nameMap_byte_enc)
	var mapdata map[string]string
	err = json.Unmarshal(nameMap_byte, &mapdata)

	return &mapdata, err
}
