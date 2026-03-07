const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.createUser = functions.https.onCall(async (data, context) => {

  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated");
  }

  const adminUid = context.auth.uid;

  const adminSnap = await admin.database()
      .ref("users/" + adminUid + "/role")
      .get();

  if (adminSnap.val() !== "App admin") {
    throw new functions.https.HttpsError("permission-denied");
  }

  const {name,email,phone,rollNo,role} = data;

  const user = await admin.auth().createUser({
    email: email,
    password: rollNo
  });

  const uid = user.uid;

  await admin.database().ref("users/"+uid).set({
    name,
    emailId: email,
    phoneNumber: phone,
    rollNo,
    role,
    uid,
    passwordChanged:false
  });

  return {uid:uid};
});