export type Tema = {
  id: string;
  titulo: string;
  paraQue: string;
  pasos: string[];
  resultado?: string;
  nota?: string;
  revision?: string;
};

export type Capitulo = {
  id: string;
  titulo: string;
  temas: Tema[];
};
