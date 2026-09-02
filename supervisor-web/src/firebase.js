import { initializeApp } from 'firebase/app'
import { getFirestore } from 'firebase/firestore'
import { getStorage } from 'firebase/storage'

const firebaseConfig = {
  apiKey: 'AIzaSyAqzbiWb5CNARBByd8iaORN3aqBwlADPuU',
  authDomain: 'checklist-choferes.firebaseapp.com',
  projectId: 'checklist-choferes',
  storageBucket: 'checklist-choferes.firebasestorage.app',
  messagingSenderId: '490914298116',
  appId: '1:490914298116:web:10103162b2e24e341f7851',
}

export const app = initializeApp(firebaseConfig)
export const db = getFirestore(app)
export const storage = getStorage(app)
