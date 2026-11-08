import 'server-only';
import {cookies} from 'next/headers';
import {isPointNetwork,type PointNetwork} from './point-network';
export async function getPointNetwork():Promise<PointNetwork|null>{
 const base=process.env.BACKEND_INTERNAL_URL;if(!base)return null;
 try{const response=await fetch(`${base.replace(/\/+$/,'')}/api/admin/red-puntos`,{headers:{Cookie:(await cookies()).toString()},cache:'no-store',redirect:'error',signal:AbortSignal.timeout(8000)});if(!response.ok)return null;const data:unknown=await response.json();return isPointNetwork(data)?data:null;}catch{return null;}
}
