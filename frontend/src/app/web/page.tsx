import {getSetupState} from "@/lib/identity-server";
import {getPublicWebsite} from "@/lib/website-server";
import {WebsiteView} from "@/components/website-view";
import "./web.css";
export const dynamic="force-dynamic";
export default async function WebsitePage(){const[setup,site]=await Promise.all([getSetupState(),getPublicWebsite()]);return <WebsiteView content={site?.contenido??null} unavailable={!site} requiresOwner={setup?.requierePropietario}/>;}
