import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '../api'
import type { User } from '../types'
export const useAuthStore=defineStore('auth',()=>{const saved=localStorage.getItem('user');const user=ref<User|null>(saved?JSON.parse(saved):null);async function login(email:string,password:string){const{data}=await api.post('/auth/login',{email,password});localStorage.setItem('token',data.token);localStorage.setItem('user',JSON.stringify(data.user));user.value=data.user}function logout(){localStorage.removeItem('token');localStorage.removeItem('user');user.value=null}return{user,login,logout}})
